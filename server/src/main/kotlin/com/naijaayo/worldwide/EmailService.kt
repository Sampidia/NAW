package com.naijaayo.worldwide

import java.util.*
import javax.mail.*
import javax.mail.internet.*

class EmailService(
    private val smtpHost: String = "mail.smtp2go.com",
    private val smtpPort: Int = 587,
    private val smtpUser: String = "hr@sampidia.com.ng",
    private val smtpPass: String = "R0ADGlPJsUh5fytK",
    private val fromEmail: String = "hr@sampidia.com.ng"
) {

    private fun createSession(): Session {
        val props = Properties().apply {
            put("mail.smtp.auth", "true")
            put("mail.smtp.starttls.enable", "true")
            put("mail.smtp.host", smtpHost)
            put("mail.smtp.port", smtpPort.toString())
            put("mail.smtp.ssl.trust", smtpHost)
        }

        return Session.getInstance(props, object : Authenticator() {
            override fun getPasswordAuthentication(): PasswordAuthentication {
                return PasswordAuthentication(smtpUser, smtpPass)
            }
        })
    }

    fun sendWelcomeEmail(toEmail: String, username: String): Boolean {
        return try {
            val session = createSession()
            val message = MimeMessage(session).apply {
                setFrom(InternetAddress(fromEmail, "Naija Ayo Worldwide"))
                addRecipient(Message.RecipientType.TO, InternetAddress(toEmail))
                subject = "Welcome to Naija Ayo Worldwide! 🌍"
                setContent(createWelcomeEmailHtml(username), "text/html; charset=utf-8")
            }

            Transport.send(message)
            println("✅ Welcome email sent successfully to $toEmail")
            true
        } catch (e: Exception) {
            println("❌ Failed to send welcome email to $toEmail: ${e.message}")
            e.printStackTrace()
            false
        }
    }

    private fun createWelcomeEmailHtml(username: String): String {
        return """
        <!DOCTYPE html>
        <html lang="en">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>Welcome to Naija Ayo Worldwide</title>
            <style>
                body {
                    font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
                    line-height: 1.6;
                    color: #333;
                    max-width: 600px;
                    margin: 0 auto;
                    background-color: #f4f4f4;
                    padding: 20px;
                }
                .container {
                    background-color: white;
                    border-radius: 10px;
                    padding: 30px;
                    box-shadow: 0 0 20px rgba(0,0,0,0.1);
                }
                .header {
                    text-align: center;
                    border-bottom: 3px solid #2E8B57;
                    padding-bottom: 20px;
                    margin-bottom: 30px;
                }
                .logo {
                    font-size: 28px;
                    font-weight: bold;
                    color: #2E8B57;
                    margin-bottom: 10px;
                }
                .welcome-message {
                    font-size: 18px;
                    color: #555;
                    margin-bottom: 20px;
                }
                .username-highlight {
                    color: #2E8B57;
                    font-weight: bold;
                    font-size: 20px;
                }
                .content-section {
                    margin: 25px 0;
                    padding: 20px;
                    background-color: #f9f9f9;
                    border-radius: 8px;
                    border-left: 4px solid #2E8B57;
                }
                .feature-list {
                    list-style: none;
                    padding: 0;
                }
                .feature-list li {
                    padding: 8px 0;
                    border-bottom: 1px solid #eee;
                }
                .feature-list li:last-child {
                    border-bottom: none;
                }
                .feature-list li:before {
                    content: "🎲";
                    margin-right: 10px;
                    color: #2E8B57;
                }
                .cta-button {
                    display: inline-block;
                    background-color: #2E8B57;
                    color: white;
                    padding: 15px 30px;
                    text-decoration: none;
                    border-radius: 5px;
                    font-weight: bold;
                    text-align: center;
                    margin: 20px 0;
                    transition: background-color 0.3s ease;
                }
                .cta-button:hover {
                    background-color: #228B22;
                }
                .footer {
                    text-align: center;
                    font-size: 12px;
                    color: #777;
                    margin-top: 30px;
                    border-top: 1px solid #eee;
                    padding-top: 20px;
                }
                .social-links {
                    margin: 15px 0;
                }
                .social-links a {
                    color: #2E8B57;
                    text-decoration: none;
                    margin: 0 10px;
                }
                @media (max-width: 600px) {
                    body {
                        padding: 10px;
                    }
                    .container {
                        padding: 20px;
                    }
                }
            </style>
        </head>
        <body>
            <div class="container">
                <div class="header">
                    <div class="logo">🌍 Naija Ayo Worldwide</div>
                    <div class="welcome-message">Welcome aboard, <span class="username-highlight">$username</span>! 🎉</div>
                </div>

                <div class="content-section">
                    <h3>Thank you for joining our community!</h3>
                    <p>You're now part of an exciting journey through Nigerian culture and strategy. Naija Ayo Worldwide brings the traditional Ayo game to your fingertips with modern multiplayer features.</p>
                </div>

                <div class="content-section">
                    <h3>What you can do now:</h3>
                    <ul class="feature-list">
                        <li>Play against AI opponents of varying difficulty</li>
                        <li>Challenge real players from around the world</li>
                        <li>Customize your avatar and profile</li>
                        <li>Track your progress on leaderboards</li>
                        <li>Experience authentic Nigerian game themes</li>
                    </ul>
                </div>

                <div class="content-section">
                    <h3>Ready to start playing?</h3>
                    <p>Jump into your first game and experience the strategic depth of Ayo, a game that's been enjoyed across Nigeria for generations!</p>
                    <a href="#" class="cta-button">Start Your First Game</a>
                </div>

                <div class="content-section">
                    <h3>Game Rules Quick Start:</h3>
                    <p><strong>Ayo</strong> is a two-player strategy game where the objective is to capture more seeds than your opponent. Players take turns sowing seeds and capturing when possible.</p>
                    <p><strong>Pro Tip:</strong> Plan ahead and watch for capture opportunities! 🧠</p>
                </div>

                <div class="footer">
                    <p><strong>Need help?</strong> Contact us at <a href="mailto:hr@sampidia.com.ng">hr@sampidia.com.ng</a></p>
                    <div class="social-links">
                        <a href="#">🌐 Website</a> |
                        <a href="#">📱 Mobile App</a> |
                        <a href="#">🎮 Game Rules</a>
                    </div>
                    <p>&copy; 2024 Naija Ayo Worldwide. Made with ❤️ for the Naija community.</p>
                    <p style="font-size: 10px; color: #999;">This email was sent to you because you registered for Naija Ayo Worldwide.</p>
                </div>
            </div>
        </body>
        </html>
        """.trimIndent()
    }

    fun sendPasswordResetEmail(toEmail: String, resetToken: String): Boolean {
        return try {
            val session = createSession()
            val message = MimeMessage(session).apply {
                setFrom(InternetAddress(fromEmail, "Naija Ayo Worldwide"))
                addRecipient(Message.RecipientType.TO, InternetAddress(toEmail))
                subject = "Reset Your Naija Ayo Worldwide Password"
                setContent(createResetPasswordEmailHtml(resetToken), "text/html; charset=utf-8")
            }

            Transport.send(message)
            println("✅ Password reset email sent successfully to $toEmail")
            true
        } catch (e: Exception) {
            println("❌ Failed to send password reset email to $toEmail: ${e.message}")
            e.printStackTrace()
            false
        }
    }

    private fun createResetPasswordEmailHtml(resetToken: String): String {
        return """
        <!DOCTYPE html>
        <html lang="en">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>Reset Your Password - Naija Ayo Worldwide</title>
            <style>
                body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; line-height: 1.6; color: #333; max-width: 600px; margin: 0 auto; background-color: #f4f4f4; padding: 20px; }
                .container { background-color: white; border-radius: 10px; padding: 30px; box-shadow: 0 0 20px rgba(0,0,0,0.1); }
                .header { text-align: center; border-bottom: 3px solid #FF6B35; padding-bottom: 20px; margin-bottom: 30px; }
                .logo { font-size: 28px; font-weight: bold; color: #FF6B35; margin-bottom: 10px; }
                .warning { background-color: #fff3cd; border: 1px solid #ffeaa7; border-radius: 5px; padding: 15px; margin: 20px 0; }
                .reset-button { display: inline-block; background-color: #FF6B35; color: white; padding: 15px 30px; text-decoration: none; border-radius: 5px; font-weight: bold; text-align: center; margin: 20px 0; }
                .footer { text-align: center; font-size: 12px; color: #777; margin-top: 30px; border-top: 1px solid #eee; padding-top: 20px; }
            </style>
        </head>
        <body>
            <div class="container">
                <div class="header">
                    <div class="logo">🔐 Naija Ayo Worldwide</div>
                    <h2>Password Reset Request</h2>
                </div>

                <div class="warning">
                    <strong>Security Notice:</strong> If you didn't request this password reset, please ignore this email. Your account is still secure.
                </div>

                <p>You requested a password reset for your Naija Ayo Worldwide account. Click the button below to reset your password:</p>

                <div style="text-align: center;">
                    <a href="#" class="reset-button">Reset My Password</a>
                </div>

                <p><small>This link will expire in 24 hours for security reasons.</small></p>

                <div class="footer">
                    <p>&copy; 2024 Naija Ayo Worldwide. Made with ❤️ for the Naija community.</p>
                </div>
            </div>
        </body>
        </html>
        """.trimIndent()
    }
}