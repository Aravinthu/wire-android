/**
 * Wire
 * Copyright (C) 2017 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.waz.zclient.utils

import java.io.UnsupportedEncodingException
import java.net.URLEncoder
import java.util.Locale

import android.app.PendingIntent
import android.content.{ClipData, Context, Intent}
import android.net.Uri
import android.text.TextUtils
import com.waz.utils.wrappers.{AndroidURIUtil, URI}
import com.waz.zclient
import com.waz.zclient.{R, ShareSavedImageActivity}
import hugo.weaving.DebugLog

object IntentUtils {

  private val WIRE_SCHEME = "wire"
  private val PASSWORD_RESET_SUCCESSFUL_HOST_TOKEN = "password-reset-successful"
  private val SMS_CODE_TOKEN = "verify-phone"
  private val INVITE_HOST_TOKEN = "connect"
  private val EXTRA_LAUNCH_FROM_SAVE_IMAGE_NOTIFICATION = "EXTRA_LAUNCH_FROM_SAVE_IMAGE_NOTIFICATION"
  private val GOOGLE_MAPS_INTENT_URI = "geo:0,0?q=%s,%s"
  private val GOOGLE_MAPS_WITH_LABEL_INTENT_URI = "geo:0,0?q=%s,%s(%s)"
  private val GOOGLE_MAPS_INTENT_PACKAGE = "com.google.android.apps.maps"
  private val GOOGLE_MAPS_WEB_LINK = "http://maps.google.com/maps?z=%d&q=loc:%f+%f+(%s)"
  private val IMAGE_MIME_TYPE = "image/*"
  private val HTTPS_SCHEME = "https"
  private val WIRE_HOST = "wire.com"

  private def schemeValid(uri: Uri): Boolean = uri != null && WIRE_SCHEME.equals(uri.getScheme)

  def isPasswordResetIntent(intent: Option[Intent]): Boolean = {
    intent match {
      case None => false
      case Some(i) =>
        val data = i.getData
        schemeValid(data) &&
        PASSWORD_RESET_SUCCESSFUL_HOST_TOKEN.equals(data.getHost)
    }
  }

  def isSmsIntent(intent: Option[Intent]): Boolean = {
    intent match {
      case None => false
      case Some(i) =>
        val data = i.getData
        schemeValid(data) &&
        SMS_CODE_TOKEN.equals(data.getHost)
    }
  }

  def isTeamAccountCreatedIntent(intent: Option[Intent]): Option[Uri] = {
    def isHttpsScheme(uri: Uri): Boolean = uri != null && HTTPS_SCHEME.equals(uri.getScheme)

    intent.flatMap { i =>
      val rex = "^/.+/download"
      val data = i.getData
      if (isHttpsScheme(data) &&
        WIRE_HOST.equals(data.getHost) &&
        data.getPath.matches(rex)) {
        Some(data)
      } else {
        None
      }
    }
  }

  @DebugLog
  def getSmsCode(intent: Option[Intent]): String = {
    intent match {
      case None => null
      case Some(i) =>
        val data = i.getData
        if(isSmsIntent(intent) &&
            data.getPath != null &&
            data.getPath.length > 1) {
          data.getPath.substring(1)
        } else {
          null
        }
    }
  }

  @DebugLog
  def getInviteToken(intent: Option[Intent]): Option[String] = {
    intent.flatMap { i =>
      val data = i.getData
      if (schemeValid(data) &&
        INVITE_HOST_TOKEN.equals(data.getHost)) {
        val token =  data.getQueryParameter("code")
        if (!TextUtils.isEmpty(token)) {
          Some(token)
        } else {
          None
        }
      } else {
        None
      }
    }
  }

  def getGalleryIntent(context: Context, uri: URI): PendingIntent = {
    // TODO: AN-2276 - Replace with ShareCompat.IntentBuilder
    val androidUri = AndroidURIUtil.unwrap(uri)
    val galleryIntent = new Intent(Intent.ACTION_VIEW)
      .setDataAndTypeAndNormalize(androidUri, IMAGE_MIME_TYPE)
      .putExtra(Intent.EXTRA_STREAM, androidUri)
    galleryIntent.setClipData(new ClipData(null, Array[String](IMAGE_MIME_TYPE), new ClipData.Item(androidUri)))
    PendingIntent.getActivity(context, 0, galleryIntent, 0)
  }

  def getPendingShareIntent(context: Context, uri: URI): PendingIntent = {
    val shareIntent = new Intent(context, classOf[ShareSavedImageActivity])
      .putExtra(Intent.EXTRA_STREAM, AndroidURIUtil.unwrap(uri))
      .putExtra(EXTRA_LAUNCH_FROM_SAVE_IMAGE_NOTIFICATION, true)
    PendingIntent.getActivity(context, 0, shareIntent, 0)
  }

  def getDebugReportIntent(context: Context, fileUri: URI): Intent = {
    val versionName = try {
        context.getPackageManager.getPackageInfo(context.getPackageName, 0).versionName
      } catch {
        case e: Exception => "n/a"
      }

    val intent = new Intent(Intent.ACTION_SEND)
      .setType("vnd.android.cursor.dir/email")
    val to =
      if (zclient.BuildConfig.DEVELOPER_FEATURES_ENABLED) {
        Array[String]{"android@wire.com"}
      } else {
        Array[String]{"support@wire.com"}
      }
    intent
      .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
      .putExtra(Intent.EXTRA_EMAIL, to)
      .putExtra(Intent.EXTRA_STREAM, AndroidURIUtil.unwrap(fileUri))
      .putExtra(Intent.EXTRA_TEXT, context.getString(R.string.debug_report__body))
      .putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.debug_report__title, versionName))
  }

  def getSavedImageShareIntent(context: Context, uri: URI): Intent = {
    val androidUri = AndroidURIUtil.unwrap(uri)
    val shareIntent = new Intent(Intent.ACTION_SEND)
      .putExtra(Intent.EXTRA_STREAM, androidUri)
      .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      .setDataAndTypeAndNormalize(androidUri, IMAGE_MIME_TYPE)
    shareIntent.setClipData(new ClipData(null, Array[String]{IMAGE_MIME_TYPE}, new ClipData.Item(androidUri)))
    Intent.createChooser(shareIntent,
                         context.getString(R.string.notification__image_saving__action__share))
  }

  def isLaunchFromSaveImageNotificationIntent(intent: Option[Intent]): Boolean = {
    intent match {
      case None => false
      case Some(i) => i.getBooleanExtra(EXTRA_LAUNCH_FROM_SAVE_IMAGE_NOTIFICATION, false)
    }
  }

  def getGoogleMapsIntent(context: Context, lat: Float, lon: Float, zoom: Int, name: String): Intent = {
    val gmmIntentUri = if (StringUtils.isBlank(name)) {
        Uri.parse(GOOGLE_MAPS_INTENT_URI.format(lat, lon))
      } else {
        Uri.parse(GOOGLE_MAPS_WITH_LABEL_INTENT_URI.format(lat, lon, name))
      }
    val mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri)
    mapIntent.setPackage(GOOGLE_MAPS_INTENT_PACKAGE)
    if (mapIntent.resolveActivity(context.getPackageManager) == null) {
      getGoogleMapsWebFallbackIntent(lat, lon, zoom, name)
    } else {
      mapIntent
    }
  }

  def getGoogleMapsWebFallbackIntent(lat: Float, lon: Float, zoom: Int, name: String): Intent = {
    val urlEncodedName = try {
        URLEncoder.encode(name, "UTF-8")
      } catch {
        case e: UnsupportedEncodingException => name
      }
    val url = GOOGLE_MAPS_WEB_LINK.formatLocal(Locale.getDefault(), zoom, lat, lon, urlEncodedName)
    new Intent(Intent.ACTION_VIEW, Uri.parse(url))
      .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
  }

  def getInviteIntent(subject: String, body: String): Intent =
    new Intent(Intent.ACTION_SEND)
    .setType("text/plain")
    .putExtra(Intent.EXTRA_SUBJECT, subject)
    .putExtra(Intent.EXTRA_TEXT, body)
}
