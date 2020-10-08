package com.github.jw3.gpkgbm

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.esri.arcgisruntime.data.GeoPackage
import com.esri.arcgisruntime.geometry.Point
import com.esri.arcgisruntime.geometry.SpatialReference
import com.esri.arcgisruntime.geometry.SpatialReferences
import com.esri.arcgisruntime.layers.FeatureLayer
import com.esri.arcgisruntime.loadable.LoadStatus
import com.esri.arcgisruntime.mapping.ArcGISMap
import com.esri.arcgisruntime.mapping.Basemap
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.nio.file.Paths


class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        checkPermissions()

        val pos: Point = currentLocation()?.let { Point(it.longitude, it.latitude, wgs84) } ?: pt0
        mapView.map = basemapFromStorage(applicationContext.filesDir)?.let {
            val m = ArcGISMap(wgs84)
            m.basemap = it
            m
        } ?: ArcGISMap(Basemap.Type.IMAGERY, pos.y, pos.x, 4)

        mapView.setViewpointCenterAsync(pos, 40000.0)
        mapView.locationDisplay.startAsync()
    }

    override fun onPause() {
        mapView.pause()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        mapView.resume()
    }

    override fun onDestroy() {
        mapView.dispose()
        super.onDestroy()
    }

    private fun checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
            + ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            + ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ),
                999
            )
        }
    }

    private fun currentLocation(): Location? {
        val loc = applicationContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return try {
            loc.getLastKnownLocation(loc.getProviders(true).first())
        } catch (e: SecurityException) {
            Log.w("gps", "unable to get initial location")
            null
        } catch (e: NoSuchElementException) {
            Log.w("gps", "unable to get initial location")
            null
        }
    }


    companion object {
        val sr = SpatialReference.create(26917)
        val wgs84 = SpatialReferences.getWgs84()
        val pt0 = Point(0.0, 0.0, sr)


        fun basemapFromStorage(dir: File?): Basemap? {
            val basemap = Basemap()
            dir?.let { GeoPackage(Paths.get(it.path, "basemap.gpkg").toString()) }
                .also { it?.loadAsync() }.also { gpkg ->
                    gpkg?.addDoneLoadingListener {
                        if (gpkg.loadStatus === LoadStatus.LOADED) {
                            gpkg.geoPackageFeatureTables.map { t -> FeatureLayer(t) }
                                .forEach { l -> basemap.baseLayers.add(l) }
                        }
                        // else error logged
                    }
                }
            return basemap
        }
    }
}