package geotrellis.raster.imagery

import geotrellis.raster._
import geotrellis.raster.render._
import geotrellis.raster.io.geotiff.SingleBandGeoTiff

import java.io.File
import spire.syntax.cfor._

object Imagery {

  def cloudlessValue(images: Array[Tile], col: Int, row: Int, threshold: Int): Int = {
    var sum = 0
    var count = 0
    cfor(0)(_ < images.length, _ + 1) { i =>
      val v = images(i).get(col, row)
      if(isData(v) && v < threshold) {
        sum += v
        count += 1
      }
    }
    sum/count
  }

  def cloudRemovalSingleBand(images: Array[Tile]) : Tile = {
    val dummyTile = images(0)
    val threshold = 10000

    dummyTile.map((col, row, x) => cloudlessValue(images, col, row, threshold))
  }

  def cloudRemovalMultiBand(images: Array[MultiBandTile]): MultiBandTile = {

    val numBands = images(0).bandCount
    val numImages = images.length

    val cloudlessTiles = new Array[Tile](numBands)

    cfor(0)(i => i < numBands, i => i + 1) { i =>
      val singleTiles = new Array[Tile](numImages)

      cfor(0)(j => j < numImages, j => j + 1) { j =>
        singleTiles(j) = images(j).band(i)
      }
      cloudlessTiles(i) = cloudRemovalSingleBand(singleTiles)
    }
    ArrayMultiBandTile(cloudlessTiles)
  }

  def main(args: Array[String]) : Unit = {
    val dirRed = new File(args(0))
    val dirGreen = new File(args(1))
    val dirBlue = new File(args(2))

    val fileListRed = dirRed.listFiles.filter(_.isFile).toList.toArray
    val fileListGreen = dirGreen.listFiles.filter(_.isFile).toList.toArray
    val fileListBlue = dirBlue.listFiles.filter(_.isFile).toList.toArray

    val numImages = fileListRed.length

    assert(numImages == fileListBlue.length && numImages == fileListGreen.length)

    val multiBands = Array.ofDim[MultiBandTile](numImages)

    cfor(0)(_ < numImages, _ + 1) { i =>
      val red = SingleBandGeoTiff(fileListRed(i).toString).tile
      val green = SingleBandGeoTiff(fileListGreen(i).toString).tile
      val blue = SingleBandGeoTiff(fileListBlue(i).toString).tile

      multiBands(i) = ArrayMultiBandTile(Array(red, green, blue))
    }

    val cloudless = cloudRemovalMultiBand(multiBands)
    cloudless.renderPng().write("/tmp/cloudless.png")
  }
}