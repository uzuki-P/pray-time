package dev.praytime.platform

import com.sun.jna.Native
import com.sun.jna.NativeLong
import com.sun.jna.platform.unix.X11
import com.sun.jna.ptr.IntByReference
import java.awt.Window

// jna-platform's X11 binding has XUngrabKeyboard but not XUngrabPointer.
private interface X11PointerControl : X11 {
    fun XUngrabPointer(display: X11.Display, time: NativeLong): Int
}

// EWMH _NET_WM_MOVERESIZE data.l[2] hint for a plain move.
private const val MOVE_HINT = 8L

object LinuxWindowMover {
    // Asks the window manager to run a native interactive move of the window,
    // as if the user had dragged its title bar. Under XWayland this is the same
    // path as a compositor drag, so it tracks the pointer exactly. Returns
    // false when it cannot work; callers should fall back to setLocation.
    fun requestInteractiveMove(window: Window): Boolean = try {
        val x11 = Native.load("X11", X11PointerControl::class.java)
        val display = x11.XOpenDisplay(null) ?: return false
        try {
            val target = X11.Window(Native.getWindowID(window))
            val root = x11.XDefaultRootWindow(display)
            val atom = x11.XInternAtom(display, "_NET_WM_MOVERESIZE", false) ?: return false
            val pointer = rootPointerPosition(x11, display, root) ?: return false

            val event = X11.XEvent()
            val client = X11.XClientMessageEvent()
            client.type = X11.ClientMessage
            client.window = target
            client.display = display
            client.message_type = atom
            client.format = 32
            client.data.setType(Array<NativeLong>::class.java)
            client.data.l = arrayOf(
                NativeLong(pointer.first.toLong()),
                NativeLong(pointer.second.toLong()),
                NativeLong(MOVE_HINT),
                NativeLong(0), // button
                NativeLong(0), // source indication: application
            )
            event.xclient = client
            event.setType(X11.XClientMessageEvent::class.java)

            // SubstructureRedirectMask | SubstructureNotifyMask: the root window
            // must forward the message to the window manager.
            val mask = NativeLong((1L shl 20) or (1L shl 21))
            x11.XSendEvent(display, root, 0, mask, event)
            // Compose holds a pointer grab from the pressed button; without
            // releasing it the WM's own grab freezes and the move never starts.
            x11.XUngrabPointer(display, NativeLong(0)) // CurrentTime
            x11.XFlush(display)
            true
        } finally {
            x11.XCloseDisplay(display)
        }
    } catch (_: Throwable) {
        false
    }

    private fun rootPointerPosition(
        x11: X11,
        display: X11.Display,
        root: X11.Window,
    ): Pair<Int, Int>? {
        val rootReturn = X11.WindowByReference()
        val childReturn = X11.WindowByReference()
        val rootX = IntByReference()
        val rootY = IntByReference()
        val winX = IntByReference()
        val winY = IntByReference()
        val mask = IntByReference()
        val inside = x11.XQueryPointer(display, root, rootReturn, childReturn, rootX, rootY, winX, winY, mask)
        return if (inside) rootX.value to rootY.value else null
    }
}
