package com.example.andy.opencv_testlicenceread.utils
import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.text.TextUtils
import android.widget.Toast
import org.opencv.core.CvException
import java.io.BufferedReader


public class StatisticsDetails
{
    var  result  = ""


    public fun  GetDetails(context: Context)
    {
        GetDetailsResult(context)
    }

    fun GetDetailsResult(context: Context)
    {
        GetCPUDetails()
        GetCpuUsageStatistic()
        GetBatteryDetails(context)
        Toast.makeText(context, result, Toast.LENGTH_LONG).show()
    }

    fun GetBatteryDetails(context: Context)
    {
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryService = context.registerReceiver(null, filter)

        //Intent receiver = context.RegisterReceiver(null, new IntentFilter(Intent.ActionBatteryChanged));
        if (batteryService != null)
        {
            var tempExtra = batteryService.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) / 10
            var level = batteryService.getIntExtra(BatteryManager.EXTRA_LEVEL, 0)

            result += "Battery Temp: " + tempExtra.toString() + "oC\nBattery level: " + level + "%"
        }
    }

    fun GetCPUDetails()
    {
        var p: Process
        try
        {
            p = Runtime.getRuntime().exec("cat sys/class/thermal/thermal_zone0/temp")
            p.waitFor()


            val reader = p.inputStream.bufferedReader().use(BufferedReader::readText)


            var temp = (reader.toFloat()) / 1000.0f

            result = "CPU Temp: " + temp.toString() + "oC\n"

        }
        catch (e: CvException) {

            //return 0.0f
        }

    }

    fun  ExecuteTop(): String
    {
        var p: Process
        var returnString = ""
        try
        {
            p = Runtime.getRuntime().exec("top -n 1")
            returnString = p.inputStream.bufferedReader().use(BufferedReader::readText).split("\n")[0]
            p.destroy()
        }
        catch (e: CvException) {
            //return 0.0f
        }

        return returnString
    }


    fun GetCpuUsageStatistic()
    {

        var tempString = ExecuteTop()

        tempString = tempString.replace(",", "")
        tempString = tempString.replace("User", "")
        tempString = tempString.replace("System", "")
        tempString = tempString.replace("IOW", "")
        tempString = tempString.replace("IRQ", "")
        tempString = tempString.replace("%", "")

        for (i in 1..10)
        {
            tempString = tempString.replace("  ", " ")
        }
        tempString = tempString.trim()

        var myString: Array<String> =  tempString.split(" ").toTypedArray()
        var cpuUsageAsInt = arrayOfNulls<Int>(myString.size)

        for (i in 1 until myString.size)
        {
            myString[i] = myString[i].trim()
            cpuUsageAsInt[i] = Integer.parseInt(myString[i])
        }
        result += "CPU Usage - User: " + cpuUsageAsInt[0].toString() + "%\n"
        result += "CPU Usage - System: " + cpuUsageAsInt[1].toString() + "%\n"
        result += "CPU Usage - IOW: " + cpuUsageAsInt[2].toString() + "%\n"
        result += "CPU Usage - IRQ: " + cpuUsageAsInt[3].toString() + "%\n"

    }

}