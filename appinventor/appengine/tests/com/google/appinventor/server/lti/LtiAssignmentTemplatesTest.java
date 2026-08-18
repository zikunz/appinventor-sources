// -*- mode: java; c-basic-offset: 2; -*-
// Copyright 2026 MIT, All rights reserved
// Released under the Apache License, Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0

package com.google.appinventor.server.lti;

import com.google.appinventor.server.LocalDatastoreTestCase;
import com.google.appinventor.server.project.youngandroid.YoungAndroidProjectService;
import com.google.appinventor.server.storage.StorageIo;
import com.google.appinventor.server.storage.StorageIoInstanceHolder;
import com.google.appinventor.shared.rpc.project.Project;
import com.google.appinventor.shared.rpc.project.TextFile;
import com.google.appinventor.shared.rpc.project.youngandroid.YoungAndroidProjectNode;

/**
 * Tests that every learner on one assignment starts from the same project.
 *
 * <p>Two things have to hold. The chosen template is frozen into a reserved account when the
 * teacher picks it, so editing the original afterwards changes nothing, and the assignment is
 * fixed to that frozen copy by its first learner, so a template picked later does not reach the
 * learners who have not opened it yet. Together these give what the classroom portal gives by
 * copying the template into an account of its own and refusing changes once a learner has started.
 *
 * @author zikun@stanford.edu (Zikun Zhu)
 */
public class LtiAssignmentTemplatesTest extends LocalDatastoreTestCase {

  private static final String ISSUER = "http://localhost:8080";
  private static final String DEPLOYMENT = "1";
  private static final String LINK_A = "42";
  private static final String LINK_B = "43";
  private static final String TEACHER = "teacher-1";

  private StorageIo storageIo;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    storageIo = StorageIoInstanceHolder.getInstance();
    storageIo.getUser(TEACHER, "teacher1@example.com");
  }

  private long createProject(String ownerId, String name) {
    Project project = new Project(name);
    project.setProjectType(YoungAndroidProjectNode.YOUNG_ANDROID_PROJECT_TYPE);
    project.addTextFile(new TextFile(YoungAndroidProjectService.PROJECT_PROPERTIES_FILE_NAME,
        "main=appinventor.ai_test." + name + ".Screen1\nname=" + name + "\n"));
    project.addTextFile(new TextFile("src/appinventor/ai_test/" + name + "/Screen1.scm", "{}"));
    return storageIo.createProject(ownerId, project, "{}");
  }

  /** Nothing is fixed until a learner opens the assignment. */
  public void testNothingIsFixedBeforeTheFirstLearner() {
    assertEquals(0, LtiAssignmentTemplates.get(ISSUER, DEPLOYMENT, LINK_A));
  }

  /** The first learner fixes the template, and a later selection does not move the assignment. */
  public void testALaterSelectionDoesNotMoveAnAssignmentUnderWay() {
    assertEquals(11L, LtiAssignmentTemplates.pin(ISSUER, DEPLOYMENT, LINK_A, 11L));
    assertEquals(11L, LtiAssignmentTemplates.pin(ISSUER, DEPLOYMENT, LINK_A, 22L));
    assertEquals(11L, LtiAssignmentTemplates.pin(ISSUER, DEPLOYMENT, LINK_A, 33L));
    assertEquals(11L, LtiAssignmentTemplates.get(ISSUER, DEPLOYMENT, LINK_A));
  }

  /** Two assignments on the same platform stay independent. */
  public void testTwoAssignmentsAreIndependent() {
    LtiAssignmentTemplates.pin(ISSUER, DEPLOYMENT, LINK_A, 11L);
    LtiAssignmentTemplates.pin(ISSUER, DEPLOYMENT, LINK_B, 22L);
    assertEquals(11L, LtiAssignmentTemplates.get(ISSUER, DEPLOYMENT, LINK_A));
    assertEquals(22L, LtiAssignmentTemplates.get(ISSUER, DEPLOYMENT, LINK_B));
  }

  /** The same assignment id under another platform or deployment is a different assignment. */
  public void testAnotherPlatformIsADifferentAssignment() {
    LtiAssignmentTemplates.pin(ISSUER, DEPLOYMENT, LINK_A, 11L);
    assertEquals(0, LtiAssignmentTemplates.get("https://other.example.org", DEPLOYMENT, LINK_A));
    assertEquals(0, LtiAssignmentTemplates.get(ISSUER, "2", LINK_A));
  }

  /**
   * An assignment created without a template is left open, so a teacher who forgot to pick one
   * can still add it. Only a real template is fixed, since that is the case that would otherwise
   * change a starting point halfway through.
   */
  public void testAnAssignmentWithNoTemplateStaysOpen() {
    assertEquals(0, LtiAssignmentTemplates.pin(ISSUER, DEPLOYMENT, LINK_A, 0));
    assertEquals(0, LtiAssignmentTemplates.get(ISSUER, DEPLOYMENT, LINK_A));
    assertEquals(9L, LtiAssignmentTemplates.pin(ISSUER, DEPLOYMENT, LINK_A, 9L));
    assertEquals(9L, LtiAssignmentTemplates.get(ISSUER, DEPLOYMENT, LINK_A));
  }
}
