package com.prayagi.netraplayer
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
class GreetingPolicyTest {
 @Test fun onlyTheChosenDayAndFirstOpen() {
  assertFalse(GreetingPolicy.eligible(LocalDate.of(2026,10,10),false))
  assertTrue(GreetingPolicy.eligible(LocalDate.of(2026,10,11),false))
  assertFalse(GreetingPolicy.eligible(LocalDate.of(2026,10,11),true))
  assertFalse(GreetingPolicy.eligible(LocalDate.of(2026,10,12),false))
 }
 @Test fun neverInterruptSilentOrMedia() {
  assertTrue(GreetingPolicy.maySpeak(false,false))
  assertFalse(GreetingPolicy.maySpeak(true,false))
  assertFalse(GreetingPolicy.maySpeak(false,true))
  assertFalse(GreetingPolicy.maySpeak(true,true))
 }
}
