// Copyright (C) 2008 The Android Open Source Project
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.gerrit.client.account;

import static com.google.gerrit.client.FormatUtil.mediumFormat;

import com.google.gerrit.client.Gerrit;
import com.google.gerrit.reviewdb.client.Account;
import com.google.gwt.i18n.client.LocaleInfo;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HTMLTable.CellFormatter;

public class MyProfileScreen extends SettingsScreen {
  private int labelIdx, fieldIdx;
  private Grid info;

  @Override
  protected void onInitUI() {
    super.onInitUI();

    if (LocaleInfo.getCurrentLocale().isRTL()) {
      labelIdx = 1;
      fieldIdx = 0;
    } else {
      labelIdx = 0;
      fieldIdx = 1;
    }

    info = new Grid((Gerrit.getConfig().siteHasUsernames() ? 1 : 0) + 4, 2);
    info.setStyleName(Gerrit.RESOURCES.css().infoBlock());
    info.addStyleName(Gerrit.RESOURCES.css().accountInfoBlock());
    add(info);

    int row = 0;
    if (Gerrit.getConfig().siteHasUsernames()) {
      infoRow(row++, Util.C.userName());
    }
    infoRow(row++, Util.C.fullName());
    infoRow(row++, Util.C.preferredEmail());
    infoRow(row++, Util.C.registeredOn());
    infoRow(row++, Util.C.accountId());

    final CellFormatter fmt = info.getCellFormatter();
    fmt.addStyleName(0, 0, Gerrit.RESOURCES.css().topmost());
    fmt.addStyleName(0, 1, Gerrit.RESOURCES.css().topmost());
    fmt.addStyleName(row - 1, 0, Gerrit.RESOURCES.css().bottomheader());
  }

  @Override
  protected void onLoad() {
    super.onLoad();
    display(Gerrit.getUserAccount());
    display();
  }

  private void infoRow(final int row, final String name) {
    info.setText(row, labelIdx, name);
    info.getCellFormatter().addStyleName(row, 0,
        Gerrit.RESOURCES.css().header());
  }

  void display(final Account account) {
    int row = 0;
    if (Gerrit.getConfig().siteHasUsernames()) {
      info.setWidget(row++, fieldIdx, new UsernameField());
    }
    info.setText(row++, fieldIdx, account.getFullName());
    info.setText(row++, fieldIdx, account.getPreferredEmail());
    info.setText(row++, fieldIdx, mediumFormat(account.getRegisteredOn()));
    info.setText(row++, fieldIdx, account.getId().toString());
  }
}
