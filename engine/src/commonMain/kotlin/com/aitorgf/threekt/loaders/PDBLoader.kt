package com.aitorgf.threekt.loaders

import com.aitorgf.threekt.core.BufferGeometry
import com.aitorgf.threekt.core.Float32BufferAttribute
import com.aitorgf.threekt.math.Color
import com.aitorgf.threekt.math.Vector3
import kotlin.js.JsExport

/** Parses atom coordinates and explicit connectivity from PDB text. */
@JsExport
class PDBLoader {
    /** Atom labels carry their CPK sRGB color as a packed RGB integer. */
    class Atom(val position: Vector3, val element: String, val color: Int)

    /** Uses typed atom records in place of the Three.js addon's raw JSON array. */
    class Result(val geometryAtoms: BufferGeometry, val geometryBonds: BufferGeometry, val atoms: Array<Atom>)

    /** Returns point positions with linear vertex colors and one line segment per unique explicit bond. */
    fun parse(text: String): Result {
        val atoms = linkedMapOf<Int, Atom>()
        val bonds = linkedSetOf<Pair<Int, Int>>()
        for (line in text.lineSequence()) {
            fun field(start: Int, end: Int) = line.take(end).drop(start).trim()
            when (field(0, 6)) {
                "ATOM", "HETATM" -> {
                    val serial = field(6, 11).toIntOrNull() ?: continue
                    val x = field(30, 38).toDoubleOrNull() ?: continue
                    val y = field(38, 46).toDoubleOrNull() ?: continue
                    val z = field(46, 54).toDoubleOrNull() ?: continue
                    val symbol = field(76, 78).ifEmpty { field(12, 14) }.lowercase()
                    atoms[serial] = Atom(Vector3(x, y, z), symbol.replaceFirstChar { it.uppercaseChar() }, cpk[symbol] ?: 0xffffff)
                }
                "CONECT" -> {
                    val start = field(6, 11).toIntOrNull() ?: continue
                    for (column in 11..26 step 5) {
                        val end = field(column, column + 5).toIntOrNull() ?: continue
                        if (start != end) bonds.add(minOf(start, end) to maxOf(start, end))
                    }
                }
            }
        }
        val positions = mutableListOf<Float>()
        val colors = mutableListOf<Float>()
        val bondPositions = mutableListOf<Float>()
        fun append(target: MutableList<Float>, vector: Vector3) {
            target.add(vector.x.toFloat())
            target.add(vector.y.toFloat())
            target.add(vector.z.toFloat())
        }
        val color = Color()
        for (atom in atoms.values) {
            append(positions, atom.position)
            color.setHex(atom.color)
            colors.add(color.r.toFloat())
            colors.add(color.g.toFloat())
            colors.add(color.b.toFloat())
        }
        for ((start, end) in bonds) {
            val a = atoms[start] ?: continue
            val b = atoms[end] ?: continue
            append(bondPositions, a.position)
            append(bondPositions, b.position)
        }
        return Result(
            BufferGeometry().setAttribute("position", Float32BufferAttribute(positions.toFloatArray(), 3))
                .setAttribute("color", Float32BufferAttribute(colors.toFloatArray(), 3)),
            BufferGeometry().setAttribute("position", Float32BufferAttribute(bondPositions.toFloatArray(), 3)),
            atoms.values.toTypedArray(),
        )
    }

    private companion object {
        val cpk = mapOf(
            "h" to 0xffffff, "he" to 0xd9ffff, "li" to 0xcc80ff, "be" to 0xc2ff00, "b" to 0xffb5b5,
            "c" to 0x909090, "n" to 0x3050f8, "o" to 0xff0d0d, "f" to 0x90e050, "ne" to 0xb3e3f5,
            "na" to 0xab5cf2, "mg" to 0x8aff00, "al" to 0xbfa6a6, "si" to 0xf0c8a0, "p" to 0xff8000,
            "s" to 0xffff30, "cl" to 0x1ff01f, "ar" to 0x80d1e3, "k" to 0x8f40d4, "ca" to 0x3dff00,
            "sc" to 0xe6e6e6, "ti" to 0xbfc2c7, "v" to 0xa6a6ab, "cr" to 0x8a99c7, "mn" to 0x9c7ac7,
            "fe" to 0xe06633, "co" to 0xf090a0, "ni" to 0x50d050, "cu" to 0xc88033, "zn" to 0x7d80b0,
            "ga" to 0xc28f8f, "ge" to 0x668f8f, "as" to 0xbd80e3, "se" to 0xffa100, "br" to 0xa62929,
            "kr" to 0x5cb8d1, "rb" to 0x702eb0, "sr" to 0x00ff00, "y" to 0x94ffff, "zr" to 0x94e0e0,
            "nb" to 0x73c2c9, "mo" to 0x54b5b5, "tc" to 0x3b9e9e, "ru" to 0x248f8f, "rh" to 0x0a7d8c,
            "pd" to 0x006985, "ag" to 0xc0c0c0, "cd" to 0xffd98f, "in" to 0xa67573, "sn" to 0x668080,
            "sb" to 0x9e63b5, "te" to 0xd47a00, "i" to 0x940094, "xe" to 0x429eb0, "cs" to 0x57178f,
            "ba" to 0x00c900, "la" to 0x70d4ff, "ce" to 0xffffc7, "pr" to 0xd9ffc7, "nd" to 0xc7ffc7,
            "pm" to 0xa3ffc7, "sm" to 0x8fffc7, "eu" to 0x61ffc7, "gd" to 0x45ffc7, "tb" to 0x30ffc7,
            "dy" to 0x1fffc7, "ho" to 0x00ff9c, "er" to 0x00e675, "tm" to 0x00d452, "yb" to 0x00bf38,
            "lu" to 0x00ab24, "hf" to 0x4dc2ff, "ta" to 0x4da6ff, "w" to 0x2194d6, "re" to 0x267dab,
            "os" to 0x266696, "ir" to 0x175487, "pt" to 0xd0d0e0, "au" to 0xffd123, "hg" to 0xb8b8d0,
            "tl" to 0xa6544d, "pb" to 0x575961, "bi" to 0x9e4fb5, "po" to 0xab5c00, "at" to 0x754f45,
            "rn" to 0x428296, "fr" to 0x420066, "ra" to 0x007d00, "ac" to 0x70abfa, "th" to 0x00baff,
            "pa" to 0x00a1ff, "u" to 0x008fff, "np" to 0x0080ff, "pu" to 0x006bff, "am" to 0x545cf2,
            "cm" to 0x785ce3, "bk" to 0x8a4fe3, "cf" to 0xa136d4, "es" to 0xb31fd4, "fm" to 0xb31fba,
            "md" to 0xb30da6, "no" to 0xbd0d87, "lr" to 0xc70066, "rf" to 0xcc0059, "db" to 0xd1004f,
            "sg" to 0xd90045, "bh" to 0xe00038, "hs" to 0xe6002e, "mt" to 0xeb0026, "ds" to 0xeb0026,
            "rg" to 0xeb0026, "cn" to 0xeb0026, "uut" to 0xeb0026, "uuq" to 0xeb0026, "uup" to 0xeb0026,
            "uuh" to 0xeb0026, "uus" to 0xeb0026, "uuo" to 0xeb0026,
        )
    }
}
