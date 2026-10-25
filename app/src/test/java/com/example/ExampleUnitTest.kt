package com.example

import com.example.model.FamilyConstants
import org.junit.Assert.assertEquals
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun testFamilyMemberOrderAndNames() {
    val expectedNames = listOf("Diverse", "Be", "To", "Li", "Lu")
    val actualNames = FamilyConstants.MEMBERS.map { it.name }
    assertEquals(expectedNames, actualNames)

    val expectedIds = listOf("diverse", "be", "to", "li", "lu")
    val actualIds = FamilyConstants.MEMBERS.map { it.id }
    assertEquals(expectedIds, actualIds)
  }

  @Test
  fun testLegacyPersonNormalization() {
    assertEquals("diverse", FamilyConstants.normalizePersonId("haushalt"))
    assertEquals("diverse", FamilyConstants.normalizePersonId("diverse"))
    assertEquals("be", FamilyConstants.normalizePersonId("bettina"))
    assertEquals("be", FamilyConstants.normalizePersonId("be"))
    assertEquals("to", FamilyConstants.normalizePersonId("tomi"))
    assertEquals("to", FamilyConstants.normalizePersonId("to"))
    assertEquals("li", FamilyConstants.normalizePersonId("lionel"))
    assertEquals("li", FamilyConstants.normalizePersonId("li"))
    assertEquals("lu", FamilyConstants.normalizePersonId("luke"))
    assertEquals("lu", FamilyConstants.normalizePersonId("lu"))
  }

  @Test
  fun testThemeModes() {
    assertEquals("Normal", com.example.model.ThemeMode.NORMAL.label)
    assertEquals("Dark Mode", com.example.model.ThemeMode.DARK.label)
    assertEquals("System", com.example.model.ThemeMode.SYSTEM.label)
  }
}
