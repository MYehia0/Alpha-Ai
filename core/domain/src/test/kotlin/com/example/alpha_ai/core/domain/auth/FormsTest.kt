package com.example.alpha_ai.core.domain.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FormsTest {

    private fun signUp(
        name: String = "Ali",
        email: String = "ali@example.com",
        password: String = "secret12",
        confirm: String = password,
    ) = SignUpForm(name, email, password, confirm)

    @Test
    fun `normalize trims and lowercases so variants compare equal`() {
        assertEquals("ali@example.com", Email.normalize("  Ali@Example.COM "))
        assertEquals(Email.normalize("ali@example.com"), Email.normalize(" ALI@example.com\t"))
    }

    @Test
    fun `email format rules`() {
        assertTrue(Email.isValid("a@b.co"))
        assertFalse(Email.isValid("a@b"))
        assertFalse(Email.isValid("@b.co"))
        assertFalse(Email.isValid("a@@b.co"))
        assertFalse(Email.isValid("a b@c.co"))
        assertFalse(Email.isValid("a@.co"))
        assertFalse(Email.isValid("a@b."))
    }

    @Test
    fun `a fully valid sign-up form has no errors`() {
        assertEquals(emptyMap<FormField, FieldError>(), signUp().validate())
    }

    @Test
    fun `whitespace-only name is empty`() {
        assertEquals(FieldError.Empty, signUp(name = "   ").validate()[FormField.Name])
    }

    @Test
    fun `empty and malformed emails`() {
        assertEquals(FieldError.Empty, signUp(email = "  ").validate()[FormField.Email])
        assertEquals(FieldError.InvalidFormat, signUp(email = "ali@").validate()[FormField.Email])
    }

    @Test
    fun `email with spaces around it and capitals is accepted`() {
        assertEquals(null, signUp(email = "  Ali@Example.com ").validate()[FormField.Email])
    }

    @Test
    fun `password length counts code points, eight is enough`() {
        assertEquals(FieldError.TooShort, signUp(password = "1234567").validate()[FormField.Password])
        assertEquals(null, signUp(password = "12345678").validate()[FormField.Password])
        // Arabic letters count once each: seven letters is too short, eight is enough.
        assertEquals(FieldError.TooShort, signUp(password = "كلمةسرق").validate()[FormField.Password])
        assertEquals(null, signUp(password = "كلمةسرقة").validate()[FormField.Password])
    }

    @Test
    fun `confirmation must match`() {
        assertEquals(
            FieldError.Mismatch,
            signUp(password = "secret12", confirm = "secret13").validate()[FormField.ConfirmPassword],
        )
    }

    @Test
    fun `sign-in needs a valid email and a password`() {
        assertEquals(emptyMap<FormField, FieldError>(), SignInForm("a@b.co", "x").validate())
        val errors = SignInForm("nope", "").validate()
        assertEquals(FieldError.InvalidFormat, errors[FormField.Email])
        assertEquals(FieldError.Empty, errors[FormField.Password])
    }

    @Test
    fun `reset needs a valid email`() {
        assertEquals(emptyMap<FormField, FieldError>(), PasswordResetForm("a@b.co").validate())
        assertEquals(FieldError.Empty, PasswordResetForm("").validate()[FormField.Email])
    }
}
