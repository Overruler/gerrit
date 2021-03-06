// Copyright (C) 2013 The Android Open Source Project
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

package com.google.gerrit.acceptance.server.change;

import static com.google.common.truth.Truth.assertThat;
import static com.google.gerrit.acceptance.GitUtil.add;
import static com.google.gerrit.acceptance.GitUtil.createCommit;
import static com.google.gerrit.acceptance.GitUtil.pushHead;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.gerrit.acceptance.AbstractDaemonTest;
import com.google.gerrit.acceptance.GitUtil.Commit;
import com.google.gerrit.acceptance.PushOneCommit;
import com.google.gerrit.acceptance.RestSession;
import com.google.gerrit.reviewdb.client.Change;
import com.google.gerrit.reviewdb.client.PatchSet;
import com.google.gerrit.server.change.GetRelated.ChangeAndCommit;
import com.google.gerrit.server.change.GetRelated.RelatedInfo;
import com.google.gerrit.server.edit.ChangeEditModifier;
import com.google.gerrit.server.edit.ChangeEditUtil;
import com.google.gerrit.server.query.change.ChangeData;
import com.google.gwtorm.server.OrmException;
import com.google.inject.Inject;

import org.eclipse.jgit.api.ResetCommand.ResetType;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

public class GetRelatedIT extends AbstractDaemonTest {
  @Inject
  private ChangeEditUtil editUtil;

  @Inject
  private ChangeEditModifier editModifier;

  @Test
  public void getRelatedNoResult() throws Exception {
    PushOneCommit push = pushFactory.create(db, admin.getIdent());
    PatchSet.Id ps = push.to(git, "refs/for/master").getPatchSetId();
    List<ChangeAndCommit> related = getRelated(ps);
    assertThat(related).isEmpty();
  }

  @Test
  public void getRelatedLinear() throws Exception {
    add(git, "a.txt", "1");
    Commit c1 = createCommit(git, admin.getIdent(), "subject: 1");
    add(git, "b.txt", "2");
    Commit c2 = createCommit(git, admin.getIdent(), "subject: 2");
    pushHead(git, "refs/for/master", false);

    for (Commit c : ImmutableList.of(c2, c1)) {
      List<ChangeAndCommit> related = getRelated(getPatchSetId(c));
      assertThat(related).hasSize(2);
      assertThat(related.get(0).changeId)
          .named("related to " + c.getChangeId()).isEqualTo(c2.getChangeId());
      assertThat(related.get(1).changeId)
          .named("related to " + c.getChangeId()).isEqualTo(c1.getChangeId());
    }
  }

  @Test
  public void getRelatedReorder() throws Exception {
    // Create two commits and push.
    add(git, "a.txt", "1");
    Commit c1 = createCommit(git, admin.getIdent(), "subject: 1");
    add(git, "b.txt", "2");
    Commit c2 = createCommit(git, admin.getIdent(), "subject: 2");
    pushHead(git, "refs/for/master", false);
    PatchSet.Id c1ps1 = getPatchSetId(c1);
    PatchSet.Id c2ps1 = getPatchSetId(c2);

    // Swap the order of commits and push again.
    git.reset().setMode(ResetType.HARD).setRef("HEAD^^").call();
    git.cherryPick().include(c2.getCommit()).include(c1.getCommit()).call();
    pushHead(git, "refs/for/master", false);
    PatchSet.Id c1ps2 = getPatchSetId(c1);
    PatchSet.Id c2ps2 = getPatchSetId(c2);

    for (PatchSet.Id ps : ImmutableList.of(c2ps2, c1ps2)) {
      List<ChangeAndCommit> related = getRelated(ps);
      assertThat(related).hasSize(2);
      assertThat(related.get(0).changeId).named("related to " + ps).isEqualTo(
          c1.getChangeId());
      assertThat(related.get(1).changeId).named("related to " + ps).isEqualTo(
          c2.getChangeId());
    }

    for (PatchSet.Id ps : ImmutableList.of(c2ps1, c1ps1)) {
      List<ChangeAndCommit> related = getRelated(ps);
      assertThat(related).hasSize(2);
      assertThat(related.get(0).changeId).named("related to " + ps).isEqualTo(
          c2.getChangeId());
      assertThat(related.get(1).changeId).named("related to " + ps).isEqualTo(
          c1.getChangeId());
    }
  }

  @Test
  public void getRelatedReorderAndExtend() throws Exception {
    // Create two commits and push.
    add(git, "a.txt", "1");
    Commit c1 = createCommit(git, admin.getIdent(), "subject: 1");
    add(git, "b.txt", "2");
    Commit c2 = createCommit(git, admin.getIdent(), "subject: 2");
    pushHead(git, "refs/for/master", false);
    PatchSet.Id c1ps1 = getPatchSetId(c1);
    PatchSet.Id c2ps1 = getPatchSetId(c2);

    // Swap the order of commits, create a new commit on top, and push again.
    git.reset().setMode(ResetType.HARD).setRef("HEAD^^").call();
    git.cherryPick().include(c2.getCommit()).include(c1.getCommit()).call();
    add(git, "c.txt", "3");
    Commit c3 = createCommit(git, admin.getIdent(), "subject: 3");
    pushHead(git, "refs/for/master", false);
    PatchSet.Id c1ps2 = getPatchSetId(c1);
    PatchSet.Id c2ps2 = getPatchSetId(c2);
    PatchSet.Id c3ps1 = getPatchSetId(c3);


    for (PatchSet.Id ps : ImmutableList.of(c3ps1, c2ps2, c1ps2)) {
      List<ChangeAndCommit> related = getRelated(ps);
      assertThat(related).hasSize(3);
      assertThat(related.get(0).changeId).named("related to " + ps).isEqualTo(
          c3.getChangeId());
      assertThat(related.get(1).changeId).named("related to " + ps).isEqualTo(
          c1.getChangeId());
      assertThat(related.get(2).changeId).named("related to " + ps).isEqualTo(
          c2.getChangeId());
    }

    for (PatchSet.Id ps : ImmutableList.of(c2ps1, c1ps1)) {
      List<ChangeAndCommit> related = getRelated(ps);
      assertThat(related).hasSize(3);
      assertThat(related.get(0).changeId).named("related to " + ps).isEqualTo(
          c3.getChangeId());
      assertThat(related.get(1).changeId).named("related to " + ps).isEqualTo(
          c2.getChangeId());
      assertThat(related.get(2).changeId).named("related to " + ps).isEqualTo(
          c1.getChangeId());
    }
  }

  @Test
  public void getRelatedEdit() throws Exception {
    add(git, "a.txt", "1");
    Commit c1 = createCommit(git, admin.getIdent(), "subject: 1");
    add(git, "b.txt", "2");
    Commit c2 = createCommit(git, admin.getIdent(), "subject: 2");
    add(git, "b.txt", "3");
    Commit c3 = createCommit(git, admin.getIdent(), "subject: 3");
    pushHead(git, "refs/for/master", false);

    Change ch2 = getChange(c2).change();
    editModifier.createEdit(ch2, getPatchSet(ch2));
    editModifier.modifyFile(editUtil.byChange(ch2).get(), "a.txt",
        RestSession.newRawInput(new byte[] {'a'}));
    String editRev = editUtil.byChange(ch2).get().getRevision().get();

    List<ChangeAndCommit> related = getRelated(ch2.getId(), 0);
    assertThat(related).hasSize(3);
    assertThat(related.get(0).changeId).named("related to " + c2.getChangeId())
        .isEqualTo(c3.getChangeId());
    assertThat(related.get(1).changeId).named("related to " + c2.getChangeId())
        .isEqualTo(c2.getChangeId());
    assertThat(related.get(1)._revisionNumber.intValue()).named(
        "has edit revision number").isEqualTo(0);
    assertThat(related.get(1).commit.commit).named(
        "has edit revision " + editRev).isEqualTo(editRev);
    assertThat(related.get(2).changeId).named("related to " + c2.getChangeId())
        .isEqualTo(c1.getChangeId());
  }

  private List<ChangeAndCommit> getRelated(PatchSet.Id ps) throws IOException {
    return getRelated(ps.getParentKey(), ps.get());
  }

  private List<ChangeAndCommit> getRelated(Change.Id changeId, int ps)
      throws IOException {
    String url = String.format("/changes/%d/revisions/%d/related",
        changeId.get(), ps);
    return newGson().fromJson(adminSession.get(url).getReader(),
        RelatedInfo.class).changes;
  }

  private PatchSet.Id getPatchSetId(Commit c) throws OrmException {
    return getChange(c).change().currentPatchSetId();
  }

  private PatchSet getPatchSet(Change c) throws OrmException {
    return db.patchSets().get(c.currentPatchSetId());
  }

  private ChangeData getChange(Commit c) throws OrmException {
    return Iterables.getOnlyElement(
        queryProvider.get().byKeyPrefix(c.getChangeId()));
  }
}
