package com.vx7.khatapro

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.vx7.khatapro.core.utils.CurrencyFormatter
import com.vx7.khatapro.core.utils.SecurityUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read app name string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("KhataPro", appName)
  }

  @Test
  fun `test password hashing and verification`() {
    val password = "masterAdminPassword123"
    val hash = SecurityUtils.hashPassword(password)
    assertTrue(SecurityUtils.verifyPassword(password, hash))
    assertTrue(!SecurityUtils.verifyPassword("wrongPassword", hash))
  }

  @Test
  fun `test calculation of order total and balance`() {
    val quantity = 5.0
    val rate = 250.0
    val total = quantity * rate
    assertEquals(1250.0, total, 0.001)

    val deposit = 500.0
    val balance = total - deposit
    assertEquals(750.0, balance, 0.001)

    val formatted = CurrencyFormatter.format(balance, "₹")
    assertEquals("₹750.00", formatted)
  }
}
