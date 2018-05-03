package com.gorakgarak.anpr.model

import org.opencv.core.Mat
import org.opencv.core.Rect

/**
 * Created by kohry on 2017-10-15.
 */
data class Plate(val img: Mat, val position: Rect, var str: String)