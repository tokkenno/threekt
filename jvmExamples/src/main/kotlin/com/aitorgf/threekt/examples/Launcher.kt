package com.aitorgf.threekt.examples

import java.awt.BorderLayout
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.util.concurrent.LinkedBlockingQueue
import javax.swing.BorderFactory
import javax.swing.DefaultListCellRenderer
import javax.swing.JButton
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JList
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.ListSelectionModel
import javax.swing.SwingUtilities
import javax.swing.WindowConstants

/**
 * Menu window that lists [Examples.all]. Swing runs on its own thread, so the chosen example is handed to
 * the main thread, which owns GLFW. Only one example runs at a time.
 */
class Launcher {
    private val requests = LinkedBlockingQueue<Request>()
    private lateinit var frame: JFrame
    private lateinit var open: JButton
    private lateinit var status: JLabel

    private sealed interface Request {
        class Open(val entry: ExampleEntry) : Request
        data object Quit : Request
    }

    /** Shows the menu and runs the chosen examples on the calling (main) thread until the menu is closed. */
    fun run() {
        SwingUtilities.invokeAndWait(::show)
        while (true) {
            val request = requests.take()
            if (request !is Request.Open) break
            setRunning(request.entry)
            try {
                ExampleWindow(request.entry).run()
            } catch (error: Exception) {
                error.printStackTrace()
                SwingUtilities.invokeLater { status.text = "No se pudo iniciar el ejemplo: ${error.message}" }
            }
            setRunning(null)
        }
    }

    private fun show() {
        val list = JList(Examples.all.toTypedArray()).apply {
            selectionMode = ListSelectionModel.SINGLE_SELECTION
            selectedIndex = 0
            cellRenderer = object : DefaultListCellRenderer() {
                override fun getListCellRendererComponent(
                    list: JList<*>?, value: Any?, index: Int, isSelected: Boolean, cellHasFocus: Boolean,
                ) = super.getListCellRendererComponent(list, (value as ExampleEntry).title, index, isSelected, cellHasFocus)
            }
        }
        open = JButton("Abrir").apply {
            addActionListener { list.selectedValue?.let { requests.put(Request.Open(it)) } }
        }
        list.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(event: MouseEvent) {
                if (event.clickCount == 2 && open.isEnabled) list.selectedValue?.let { requests.put(Request.Open(it)) }
            }
        })
        status = JLabel("Elige un ejemplo y pulsa Abrir.")
        val buttons = JPanel(BorderLayout(8, 0)).apply {
            add(status, BorderLayout.CENTER)
            add(open, BorderLayout.EAST)
        }
        frame = JFrame("Ejemplos de Threekt").apply {
            defaultCloseOperation = WindowConstants.DISPOSE_ON_CLOSE
            addWindowListener(object : WindowAdapter() {
                override fun windowClosed(event: WindowEvent) = requests.put(Request.Quit)
            })
            contentPane = JPanel(BorderLayout(0, 8)).apply {
                border = BorderFactory.createEmptyBorder(12, 12, 12, 12)
                add(JScrollPane(list), BorderLayout.CENTER)
                add(buttons, BorderLayout.SOUTH)
            }
            setSize(420, 320)
            setLocationRelativeTo(null)
            isVisible = true
        }
    }

    private fun setRunning(entry: ExampleEntry?) = SwingUtilities.invokeLater {
        open.isEnabled = entry == null
        status.text = if (entry == null) "Elige un ejemplo y pulsa Abrir." else "Ejecutando ${entry.title}…"
    }
}
