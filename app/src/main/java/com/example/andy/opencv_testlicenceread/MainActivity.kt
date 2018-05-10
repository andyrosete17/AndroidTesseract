package com.example.andy.opencv_testlicenceread

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.support.v4.app.ActivityCompat
import android.support.v4.content.FileProvider
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.text.TextUtils.replace
import android.util.Log
import android.view.WindowManager
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import com.example.andy.opencv_testlicenceread.utils.ProvinceEnum
import com.googlecode.tesseract.android.TessBaseAPI
import com.gorakgarak.anpr.model.Plate
import kotlinx.android.synthetic.main.activity_main.*
import org.opencv.android.*
import org.opencv.core.*
import org.opencv.core.Core.*
import org.opencv.core.CvType.CV_8UC3
import org.opencv.imgproc.Imgproc
import org.opencv.imgproc.Imgproc.*
import java.text.SimpleDateFormat
import java.util.*
import org.opencv.android.Utils;
import org.opencv.core.Point
import java.io.*
import java.util.regex.Pattern


var image: Bitmap ?= Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)

var mTess : TessBaseAPI?= null
var datapath :String= ""
private var textView: TextView ?= null
lateinit var imageFilePath: String
private val GALLERY = 1
private val CAMERA = 2
var imageLoaded : Boolean = false
private lateinit var listView: ListView

var time: Long = System.currentTimeMillis()

class MainActivity : AppCompatActivity() {
   // private var _cameraBridgeViewBase: CameraBridgeViewBase? = null


    private var _baseLoaderCallback = object : BaseLoaderCallback(this)
    {
        override fun onManagerConnected(status: Int)
        {
            when (status)
            {
                LoaderCallbackInterface.SUCCESS ->
                {
                    Log.i(TAG, "OpenCV loaded successfully")
                    // Load ndk built module, as specified in moduleName in build.gradle
                    // after opencv initialization
                    System.loadLibrary("native-lib")
                    //_cameraBridgeViewBase!!.enableView()
                }
                else ->
                {
                    super.onManagerConnected(status)
                }
            }
        }
    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(R.layout.activity_main)

        // Permissions for Android 6+
        ActivityCompat.requestPermissions(this@MainActivity,
                arrayOf(Manifest.permission.CAMERA),
                1)

        datapath = filesDir.toString() + "/tesseract/"

        //make sure training data has been copied
        checkFile(File(datapath + "tessdata/"))

        //initialize Tesseract API
        val lang = "eng"
        mTess = TessBaseAPI()
        mTess!!.init(datapath, lang)

            //Prepare the button to take an image
            cameraButton.setOnClickListener()
            {
                showPictureDialog()
            }

            tesseractBtn.setOnClickListener()
            {
                TesseractExecution()
            }
    }


//    public override fun onPause() {
//        super.onPause()
//        disableCamera()
//    }
//
    public override fun onResume() {
        super.onResume()
        if (!OpenCVLoader.initDebug()) {
            Log.i(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization")
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, _baseLoaderCallback)
        } else {
            Log.i(TAG, "OpenCV library found inside package. Using it!")
            _baseLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            1 -> {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Toast.makeText(this@MainActivity, "Permission denied to read your External storage", Toast.LENGTH_SHORT).show()
                }
                return
            }
        }// other 'case' lines to check for other
        // permissions this app might request
    }

    private fun getTimeDiff(): Long {
        val diff = System.currentTimeMillis() - time
        time = System.currentTimeMillis()
        return diff
    }

    private fun initTimer() {
        time = System.currentTimeMillis()
    }


    private fun getPlateCandidatesFromImage(input: Mat, result: Mat, rects: MutableList<RotatedRect>): List<Plate> {

        val output = mutableListOf<Plate>()

        Log.i(TAG, " ${rects.size} rects found")

        rects.forEach { rect ->

            initTimer()
/*
            //This part is for get minimum rectangle using mask.
            //very very slow.

//            Log.i(TAG, "   2-1) ${getTimeDiff()}")
//            //For better rect cropping for each possible box
//            //Make floodfill algorithm because the plate has white background
//            //And then we can retrieve more clearly the contour box
//            circle(result, rect.center, 3, Scalar(0.0, 255.0, 0.0), -1);
//
//            val minSize = if (rect.size.width < rect.size.height) rect.size.width * 0.5 else rect.size.height * 0.5
//
//            var mask = Mat()
//            mask.create(input.rows() + 2, input.cols() + 2, CvType.CV_8UC1)
//            mask = Mat.zeros(mask.size(), CvType.CV_8UC1)
//
//            val loDiff = 30.0
//            val upDiff = 30.0
//            val connectivity = 4
//            val newMaskVal = 255
//            val seedNum = 10
//            val ccomp: Rect = Rect()
//            val flags = connectivity + (newMaskVal.shl(8) + FLOODFILL_FIXED_RANGE + FLOODFILL_MASK_ONLY)
//
//            (0..seedNum).forEach { sn ->
//                val num = Random().nextInt()
//                val seed: Point = Point()
//                seed.x = rect.center.x + num % (minSize - (minSize / 2))
//                seed.y = rect.center.y + num % (minSize - (minSize / 2))
//                circle(result, seed, 1, Scalar(0.0, 255.0, 255.0), -1);
//                val area = floodFill(input, mask, seed, Scalar(255.0, 0.0, 0.0), ccomp, Scalar(loDiff, loDiff, loDiff), Scalar(upDiff, upDiff, upDiff), flags)
//            }
//
//            Log.i(TAG, "   2-2) ${getTimeDiff()} after FLOODFILL ")
//
//            //Check new floodfill mask match for a correct patch.
//            //Get all points detected for get Minimal rotated Rect
////            val pointsInterestList: MutableList<Point> = mutableListOf()
//
//            val pointsInterestList: MutableList<Point> = arrayListOf()
//
//            (0 until mask.cols()).forEach { col ->
//                (0 until mask.rows()).forEach { row ->
//                    if (mask.get(row, col)[0] == 255.0) {
//                        pointsInterestList.add(Point(col.toDouble(), row.toDouble()))
//                    }
//                }
//            }
//
//            Log.i(TAG, "   2-3) ${getTimeDiff()} after MASKING ")
//
//            val m2fFromList = MatOfPoint2f()
//            m2fFromList.fromList(pointsInterestList) //create MatOfPoint2f from list of points
//            val m2f = MatOfPoint2f()
//            m2fFromList.convertTo(m2f, CvType.CV_32FC2) //convert to type of MatOfPoint2f created from list of points
//
//            val minRect = Imgproc.minAreaRect(m2fFromList)

//            if (verifySizes(minRect)) {
*/
            val minRect = rect

            if (verifySizes(minRect)) {

                // rotated rectangle drawing
                val rectPoints: Array<Point> = arrayOf<Point>(Point(), Point(), Point(), Point())
//            val rectPoints = MatOfPoint2f().toArray()

                minRect.points(rectPoints)

                (0 until 4).forEach { line(result, rectPoints[it], rectPoints[(it + 1) % 4], Scalar(0.0, 0.0, 255.0), 3) }

                val r = minRect.size.width / minRect.size.height
                var angle = minRect.angle
                if (r < 1) angle += 90

                val rotatedMat = Mat()
                /*
                * Applies an affine transformation to an image.
                * Parameters:
                    src – input image.
                    dst – output image that has the size dsize and the same type as src .
                    M – 2\times 3 transformation matrix.
                    dsize – size of the output image.
                    flags – combination of interpolation methods (see resize() ) and the optional flag WARP_INVERSE_MAP that means that M is the inverse transformation ( \texttt{dst}\rightarrow\texttt{src} ).
                    borderMode – pixel extrapolation method (see borderInterpolate()); when borderMode=BORDER_TRANSPARENT , it means that the pixels in the destination image corresponding to the “outliers” in the source image are not modified by the function.
                    borderValue – value used in case of a constant border; by default, it is 0.

                * */
                warpAffine(input, rotatedMat, getRotationMatrix2D(minRect.center, angle, 1.0), input.size(), INTER_CUBIC) //TODO: This must be the same

                Log.i(TAG, "   2-4) ${getTimeDiff()} after WARP AFFINE ")

                val rectSize = minRect.size
                if (r < 1) {
                    val h = rectSize.height
                    val w = rectSize.width
                    rectSize.height = w
                    rectSize.width = h
                }

                val cropMat = Mat()
/*Retrieves a pixel rectangle from an image with sub-pixel accuracy.
Para dibujar el rectángulo
* Parameters:

src – Source image.
patchSize – Size of the extracted patch.
center – Floating point coordinates of the center of the extracted rectangle within the source image. The center must be inside the image.
dst – Extracted patch that has the size patchSize and the same number of channels as src .
patchType – Depth of the extracted pixels. By default, they have the same depth as src .

* */
                getRectSubPix(rotatedMat, rectSize, minRect.center, cropMat)

                val resizedResultMat = Mat()
                resizedResultMat.create(33, 144, CV_8UC3)
                resize(cropMat, resizedResultMat, resizedResultMat.size(), 0.0, 0.0, INTER_CUBIC)

                //Equalized cropped image
                var grayResultMat = Mat()
                cvtColor(resizedResultMat, grayResultMat, COLOR_BGR2GRAY) //TODO: Check constants
                //blur(grayResultMat, grayResultMat, Size(3.0, 3.0))
                grayResultMat = histeq(grayResultMat)

                Log.i(TAG, "   2-5) ${getTimeDiff()} after EQUALIZING CROP ")

                //Plate Candidates Here
                output.add(Plate(grayResultMat, minRect.boundingRect(), ""))

            }

        }

        Log.i(TAG, "   2-6) ${output.size} output ")

        return output

    }

    private fun histeq(input: Mat): Mat {
        val output = Mat(input.size(), input.type())
        when (input.channels()) {
            3 -> {
                val hsv = Mat()
                val hsvSplit: List<Mat> = emptyList()
                cvtColor(input, hsv, COLOR_BGR2HSV)
                split(hsv, hsvSplit)
                equalizeHist(hsvSplit[2], hsvSplit[2])
                merge(hsvSplit, hsv)
                cvtColor(hsv, output, COLOR_HSV2BGR)
            }
            1 -> equalizeHist(input, output)
        }
        return output
    }


    private fun verifySizes(candidate: RotatedRect): Boolean {

        val error = 0.4
        val aspect = 4.7272
        val min = 15 * aspect * 15
        val max = 125 * aspect * 125
        val rmin = aspect - aspect * error
        val rmax = aspect + aspect * error
        val area = candidate.size.height * candidate.size.width

        var r = candidate.size.width / candidate.size.height
        if (r < 1) r = 1 / r

        return !((area < min || area > max) || (r < rmin || r > rmax))

    }

    private fun SaveImageMAT(subimg:Mat?, text:String)
    {
        val TAG = MainActivity.TAG
        var bmp: Bitmap? = null
        try {
            bmp = Bitmap.createBitmap(subimg!!.cols(), subimg!!.rows(), Bitmap.Config.ARGB_8888)
            Utils.matToBitmap(subimg, bmp)
        } catch (e: CvException) {
            Log.i(TAG, e.message)
        }


       // subimg!!.release()


        var out: FileOutputStream? = null

        val currentTime = java.util.Calendar.getInstance()[14]
        val filename = "$text.frame$currentTime.png"


        val sd = File("${Environment.getExternalStorageDirectory()}/frames")
        var success = true
        if (!sd.exists()) {
            success = sd.mkdir()
        }
        if (success) {
            val dest = File(sd, filename)

            try {
                out = FileOutputStream(dest)
                bmp!!.compress(Bitmap.CompressFormat.PNG, 100, out) // bmp is your Bitmap instance
                // PNG is a lossless format, the compression factor (100) is ignored

            } catch (e: Exception) {
                e.printStackTrace()
                Log.i(TAG, e.message)
            } finally {
                try {
                    if (out != null) {
                        out!!.close()
                        Log.i(TAG, "OK!!")
                    }
                } catch (e: IOException) {
                    Log.i(TAG, e.message + "Error")
                    e.printStackTrace()
                }

            }
        }
    }

    private fun SaveImageBitMap(subimg:Bitmap?, text:String)
    {
        val TAG = MainActivity.TAG
        var out: FileOutputStream? = null

        val currentTime = java.util.Calendar.getInstance()[14]
        val filename = "$text.frame$currentTime.png"


        val sd = File("${Environment.getExternalStorageDirectory()}/frames")
        var success = true
        if (!sd.exists()) {
            success = sd.mkdir()
        }
        if (success) {
            val dest = File(sd, filename)

            try {
                out = FileOutputStream(dest)
                subimg!!.compress(Bitmap.CompressFormat.PNG, 100, out) // bmp is your Bitmap instance
                // PNG is a lossless format, the compression factor (100) is ignored

            } catch (e: Exception) {
                e.printStackTrace()
                Log.i(TAG, e.message)
            } finally {
                try {
                    if (out != null) {
                        out!!.close()
                        Log.i(TAG, "OK!!")
                    }
                } catch (e: IOException) {
                    Log.i(TAG, e.message + "Error")
                    e.printStackTrace()
                }

            }
        }
    }


    fun checkFile( dir :File)
    {
        //directory does not exist, but we can successfully create it
        if (!dir.exists() && dir.mkdirs()) {
            copyFiles()
        }
        //The directory exists, but there is no data file in it
        if (dir.exists()) {
            val datafilepath = datapath + "/tessdata/eng.traineddata"
            val datafile = File(datafilepath)
            if (!datafile.exists()) {
                copyFiles()
            }
        }
    }


    fun copyFiles ()
    {
        try {
            //location we want the file to be at
            val filepath = datapath + "/tessdata/eng.traineddata"

            //get access to AssetManager
            val assetManager = assets

            //open byte streams for reading/writing
            val instream = assetManager.open("testdata/eng.traineddata")
            val outstream = FileOutputStream(filepath)

            //copy the file to the location specified by filepath
            val buffer = ByteArray(1024)
            var read: Int
            read = instream.read(buffer)
            while (read != -1)
            {

                outstream.write(buffer, 0, read)
                read = instream.read(buffer)
            }
            outstream.flush()
            outstream.close()
            instream.close()

        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }

    }

    fun processImage(image : Bitmap?) : String?
    {
        val OCRresult: String?
        mTess?.setImage(image)
        OCRresult = mTess?.getUTF8Text()
        return OCRresult
        mTess!!.end()
    }

    companion object {

        private val TAG = "OCVSample::Activity"
    }


    private fun showPictureDialog() {
        val pictureDialog = AlertDialog.Builder(this)
        pictureDialog.setTitle("Select Action")
        val pictureDialogItems = arrayOf("Select photo from gallery", "Capture photo from camera")
        pictureDialog.setItems(pictureDialogItems
        ) { dialog, which ->
            when (which) {
                0 -> choosePhotoFromGallary()
                1 -> takePhotoFromCamera()
            }
        }
        pictureDialog.show()
    }


    fun choosePhotoFromGallary() {
        val galleryIntent = Intent(Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI)

        startActivityForResult(galleryIntent, GALLERY)
    }

    private fun takePhotoFromCamera() {
        try {
            val imageFile = createImageFile()
            val callCameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            if (callCameraIntent.resolveActivity(packageManager) != null)
            {
                val authorities = packageName + ".fileprovider"
                val imageURI = FileProvider.getUriForFile(this, authorities, imageFile)
                callCameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageURI)
                startActivityForResult(callCameraIntent, CAMERA)
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Could not create file!", Toast.LENGTH_SHORT).show()
        }
    }

    //Take an image
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when(requestCode)
        {
            CAMERA -> {
                if (resultCode == Activity.RESULT_OK){
                    photoImage.setImageBitmap(setScaleBitmap())
                    imageLoaded = true
                }
            }
            GALLERY ->
            {
                if (data != null)
                {
                    val contentURI = data!!.data
                    imageFilePath =data.data.path
                    try
                    {
                        val selectedImageURI = data.data
                        val imageFile = File(getRealPathFromURI(selectedImageURI))
                        imageFilePath = imageFile.absolutePath
                        photoImage.setImageBitmap(setScaleBitmap())
                        imageLoaded = true
                    }
                    catch (e: IOException) {
                        e.printStackTrace()
                        Toast.makeText(this@MainActivity, "Failed!", Toast.LENGTH_SHORT).show()
                        imageLoaded = false
                    }
                }
            }
            else->
            {
                Toast.makeText(this, "Unrecognize request code", Toast.LENGTH_SHORT).show()
                imageLoaded = false
            }
        }
    }

    //Create a file image and folder
    @Throws(IOException::class) //throw exception if something fail
    fun createImageFile() : File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val imageFileName = "JPEG_" + timeStamp + "_"
        val storageDirectory = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        if (!storageDirectory.exists())
            storageDirectory.mkdir()

        val imageFile = createTempFile(imageFileName, ".jpg", storageDirectory)
        imageFilePath = imageFile.absolutePath

        return imageFile
    }

    fun setScaleBitmap() : Bitmap {
        val imageViewWidth = photoImage.width
        val imageViewHeight = photoImage.height

        val bmOptions = BitmapFactory.Options()
        bmOptions.inJustDecodeBounds = true
        BitmapFactory.decodeFile(imageFilePath, bmOptions)
        val bitmapWidth = bmOptions.outWidth
        val bitmapHeight = bmOptions.outHeight

        val scaleFactor = Math.min(bitmapWidth/imageViewWidth, bitmapHeight/imageViewHeight)
        bmOptions.inSampleSize = scaleFactor
        bmOptions.inJustDecodeBounds = false

        return BitmapFactory.decodeFile(imageFilePath, bmOptions)
    }

    private fun getRealPathFromURI(contentURI: Uri): String {
        val result: String
        val cursor = contentResolver.query(contentURI, null, null, null, null)
        if (cursor == null) { // Source is Dropbox or other similar local file path
            result = contentURI.getPath()
        } else {
            cursor.moveToFirst()
            val idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA)
            result = cursor.getString(idx)
            cursor.close()
        }
        return result
    }

    fun TesseractExecution() : List<String>
    {
        var results : List<String> = emptyList()
        var  matResult = Mat()

        if (imageLoaded)
        {
            val bitmap = (photoImage.getDrawable() as BitmapDrawable).bitmap
            SaveImageBitMap(bitmap,"bitmapOriginal")
            matResult = DetectLicencePlate(bitmap)
            var bitmapResult: Bitmap = Bitmap.createBitmap(matResult!!.cols(), matResult!!.rows(), Bitmap.Config.ARGB_8888)
            Utils.matToBitmap(matResult, bitmapResult)
            photoImage.setImageBitmap(bitmapResult)

        }

        return results
    }

    private fun DetectLicencePlate(bitmapInput : Bitmap): Mat {


        var  matGray = Mat()
        var  matCanny = Mat()
        var  matOriginal = Mat(bitmapInput.height,bitmapInput.width, CvType.CV_8UC1)

        Utils.bitmapToMat(bitmapInput, matOriginal)
        SaveImageMAT(matOriginal,"MatOriginal")

        Log.i(TAG, "1-1) GrayScale & blur for noise removal")
        //Grayscale of the original image Mat
        Imgproc.cvtColor(matOriginal, matGray, Imgproc.COLOR_BGR2GRAY,4)
        SaveImageMAT(matGray,"MatGray")

        Log.i(TAG, "1-2) Sobel")
        Canny(matGray, matCanny,100.0,50.0,3,false)
        Log.i(TAG, "1-5) Find contour of possible plates")
        val contourList: List<MatOfPoint> = mutableListOf()
        val hierarchy = Mat()
        /*
        * Finds contours in a binary image.

        The function retrieves contours from the binary image using the algorithm [Suzuki85]. The contours are a useful tool for shape analysis and object detection and recognition. See squares.c in the OpenCV sample directory.
        Source image is modified by this function. Also, the function does not take into account 1-pixel border of the image (it's filled with 0's and used for neighbor analysis in the algorithm), therefore the contours touching the image border will be clipped.
        Parameters:
            image - Source, an 8-bit single-channel image. Non-zero pixels are treated as 1's. Zero pixels remain 0's, so the image is treated as binary. You can use "compare", "inRange", "threshold", "adaptiveThreshold", "Canny", and others to create a binary image out of a grayscale or color one. The function modifies the image while extracting the contours.

            contours - Detected contours. Each contour is stored as a vector of points.
    hierarchy - Optional output vector, containing information about the image topology. It has as many elements as the number of contours. For each i-th contour contours[i], the elements hierarchy[i][0], hiearchy[i][1], hiearchy[i][2], and hiearchy[i][3] are set to 0-based indices in contours of the next and previous contours at the same hierarchical level, the first child contour and the parent contour, respectively. If for the contour i there are no next, previous, parent, or nested contours, the corresponding elements of hierarchy[i] will be negative.

            mode - Contour retrieval mode (if you use Python see also a note below).

        CV_RETR_EXTERNAL retrieves only the extreme outer contours. It sets hierarchy[i][2]=hierarchy[i][3]=-1 for all the contours.
        CV_RETR_LIST retrieves all of the contours without establishing any hierarchical relationships.
        CV_RETR_CCOMP retrieves all of the contours and organizes them into a two-level hierarchy. At the top level, there are external boundaries of the components. At the second level, there are boundaries of the holes. If there is another contour inside a hole of a connected component, it is still put at the top level.
        CV_RETR_TREE retrieves all of the contours and reconstructs a full hierarchy of nested contours. This full hierarchy is built and shown in the OpenCV contours.c demo.

    method - Contour approximation method (if you use Python see also a note below).

        CV_CHAIN_APPROX_NONE stores absolutely all the contour points. That is, any 2 subsequent points (x1,y1) and (x2,y2) of the contour will be either horizontal, vertical or diagonal neighbors, that is, max(abs(x1-x2),abs(y2-y1))==1.
        CV_CHAIN_APPROX_SIMPLE compresses horizontal, vertical, and diagonal segments and leaves only their end points. For example, an up-right rectangular contour is encoded with 4 points.
        CV_CHAIN_APPROX_TC89_L1,CV_CHAIN_APPROX_TC89_KCOS applies one of the flavors of the Teh-Chin chain approximation algorithm. See [TehChin89] for details.

    offset - Optional offset by which every contour point is shifted. This is useful if the contours are extracted from the image ROI and then they should be analyzed in the whole image context.
        * */
        Imgproc.findContours(matCanny, contourList, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE)


        Log.i(TAG, "1-6) Get rectangle from the contours")
        val rectList: MutableList<RotatedRect> = mutableListOf()

        for (contour in contourList)
        {
            if(Imgproc.contourArea(contour)>400)
            {
                val p = MatOfPoint2f()
                contour.convertTo(p, CvType.CV_32F)
                val img = Imgproc.minAreaRect(p)
                if (verifySizes(img)) rectList.add(Imgproc.minAreaRect(p))
            }
        }


        //draw contour on original colored image to fetch white number plate.
       // val input = inputFrame.rgba()
        val input = matOriginal
        cvtColor(input, input, COLOR_RGBA2RGB)
        val result = Mat()
        input.copyTo(result)

        Log.i(TAG, "1-7) Floodfill algorithm from more clear contour box, get plates candidates")
        val plateCandidates = getPlateCandidatesFromImage(input, result, rectList)
        var textList  = emptyList<String>()
        var validWords = emptyList<String>()
        for (plate in plateCandidates)
        {
            plate.str = ""
            val extra = Mat()
            //plate.img.copyTo(extra)

            plate.img.copyTo(extra)
            SaveImageMAT(extra,"matExtra")

            for (i in 1..3)
            {
               when(i)
               {
                   1 ->
                   {
                         val temp = FindLicensePlate(extra, 80.0)
                       if (!temp.isNullOrEmpty())
                           textList += temp
                   }
                   2 ->
                   {
                       val temp = FindLicensePlate(extra, 100.0)
                       if (!temp.isNullOrEmpty())
                           textList += temp
                   }
                   3 ->
                   {
                       val temp = FindLicensePlate(extra, 120.0)
                       if (!temp.isNullOrEmpty())
                           textList += temp
                   }
               }
            }

        }
            textList
                    .asSequence()
                    .filter { it.length >= 6 }
                    .map { FilterLicenceSpain(it) }
                    .filter { !it.isNullOrEmpty() && !validWords.contains(it) }
                    .forEach { validWords += it }

            if (validWords.count()>0)
            {
                val listItems = arrayOfNulls<String>(validWords.size)
                listView =findViewById(R.id.recipe_list_view)
                for(i in 0 until validWords.size)
                {
                        listItems[i] =validWords[i]
                }
                val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, listItems)
                listView.adapter = adapter
            }


        return result
    }

    private fun FilterLicenceSpain(word: String): String
    {
        var result = ""
        var replacement = word
        var mask = emptyList<String>()
        var charList = replacement.toCharArray()

        for (character in charList)
        {
            mask += if (character.isDigit()) {
                "0"
            } else {
                "1"
            }
        }

        if (mask.count() >= 8)
        {
            if(CompareCharList(mask, 6, "100001"))
            {
                replacement = replacement.substring(replacement.length -6)
                mask = GerenateMak(mask, 6, false)
            }
            else if (CompareCharList(mask, 7, "1100001")
                    ||CompareCharList(mask, 7, "1000011")
                    || CompareCharList(mask, 7, "0000111"))
            {
                replacement = replacement.substring(replacement.length -7)
                mask = GerenateMak(mask, 7, false)
            }
            else if ( CompareCharList(mask, 8, "11000011")
                    || CompareCharList(mask, 8, "10000011"))
            {
                replacement = replacement.substring(replacement.length -8)
                mask = GerenateMak(mask, 8, false)
            }
            else if (charList.toString().indexOf("111")>0)
            {
                replacement = replacement.substring(0, mask.toString().indexOf("111") +3 )
                mask = GerenateMak(mask, 7, true)
            }
        }

        var maskString = GetStringFromList(mask)

        when(maskString.count())
        {
            6->
            {
                if (maskString == "100001")
                {
                    if (CheckProvinceEnum(replacement.substring(0,1)))
                    {
                        result = replacement
                    }
                }
            }
            7 ->
            {
                if (maskString == "0000111")
                {
                    result = replacement
                }

                if (maskString == "1100001")
                {
                    if (CheckProvinceEnum(replacement.substring(0,2)))
                    {
                        result = replacement
                    }
                }

                if (maskString == "1000011")
                {
                    if (CheckProvinceEnum(replacement.substring(0,1)))
                    {
                        result = replacement
                    }
                }
            }
            8->
            {
                if (maskString == "11000011")
                {
                    if (CheckProvinceEnum(replacement.substring(0,2)))
                    {
                        result = replacement
                    }
                }
                if (maskString == "10000011")
                {
                    if (CheckProvinceEnum(replacement.substring(0,1)))
                    {
                        result = replacement
                    }
                }
            }
        }

        return result
    }

    private  fun GetStringFromList(list:List<String>):String
    {
        val text = list.toString()
        var result = ""
        val pattern = Pattern.compile("\t|\n|\r")
        val pattern2 = Pattern.compile("[^0-9a-zA-Z]")
        result = text.replace(pattern.toRegex(),"")
        result =  text.replace(pattern2.toRegex(),"")

        return result
    }

    private fun CompareCharList(mask: List<String>, limit: Int, compareExpression : String): Boolean
    {
        var result = false
        val maskString =GetStringFromList(mask)
        if (maskString.substring(mask.count()-limit) == compareExpression)
        {
            result = true
        }
        return result
    }

    private fun CheckProvinceEnum(provinceSufix: String): Boolean
    {
        var result = false

        try {
            var province = ProvinceEnum.valueOf(provinceSufix)
            result = true
        }
        catch (e : Exception){}

        return result
    }

    private fun GerenateMak(mask: List<String>, limit: Int, direction: Boolean): List<String>
    {
       var  maskTemp = emptyList<String>()
        var maskString = GetStringFromList(mask)
         if (direction)
            {
                for (i  in 1 until limit)
                {
                    maskTemp += mask[i]
                }
            }
            else
            {
                for (i in maskString.substring(mask.count()-limit))
                {
                    maskTemp += i.toString()
                }
            }

            return maskTemp
    }

    private fun FindLicensePlate(extra: Mat, threshold_parameter : Double): String {
        var results  = ""
        val final = Mat()
        val matThreshold = Mat()
        val pattern = Pattern.compile("\t|\n|\r")
        val pattern2 = Pattern.compile("[^0-9a-zA-Z]")
        Imgproc.threshold(extra, matThreshold, threshold_parameter, 255.0, Imgproc.THRESH_BINARY_INV)
        SaveImageMAT(matThreshold,"MatThreshold")


        var image2: Bitmap = Bitmap.createBitmap(matThreshold!!.cols(), matThreshold!!.rows(), Bitmap.Config.ARGB_8888)

        matThreshold.copyTo(final)

        Utils.matToBitmap(final, image2)
        SaveImageBitMap(image2, "bitmapFinal")
        val Text = processImage(image2)
        if (Text!!.length>3)
        {
            results = Text.replace(pattern.toRegex(),"")
            results =results.replace(pattern2.toRegex(),"")
        }

        return results
    }


}
