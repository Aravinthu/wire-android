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
package com.waz.zclient

import android.content.Intent
import android.os.Bundle
import com.waz.zclient.utils.IntentUtils
import com.waz.zclient.Intents._

class SMSCodeReceiverActivity extends BaseActivity {

  override def onCreate(savedInstanceState: Bundle) = {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_smscode_receiver)
  }

  override def onStart() = {
    super.onStart()
    val intent = Option(getIntent)
    if(IntentUtils.isSmsIntent(intent)){
      forwardSmsCode(intent)
    }
  }

  override def onNewIntent(intent: Intent) = {
    super.onNewIntent(intent)
    if(IntentUtils.isSmsIntent(intent.toOpt)) {
      setIntent(intent)
      forwardSmsCode(intent.toOpt)
    }
  }

  private def forwardSmsCode(intent: Option[Intent]) = {
    getControllerFactory()
      .getVerificationController
      .setVerificationCode(IntentUtils.getSmsCode(intent))
    finish()
  }
}
