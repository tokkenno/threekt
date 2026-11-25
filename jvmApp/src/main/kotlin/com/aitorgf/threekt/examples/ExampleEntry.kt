package com.aitorgf.threekt.examples

import com.aitorgf.threekt.controls.InputSurface
import com.aitorgf.threekt.renderers.webgl.GLContext

/** An example in the menu: its upstream Three.js [id], a Spanish [title], and its factory. */
class ExampleEntry(val id: String, val title: String, val create: (GLContext, InputSurface) -> Example)
