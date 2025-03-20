package com.example.hw5

import java.util.*
import javax.mail.*
import javax.mail.internet.*


fun main(args: Array<String>) {
    if (args.isEmpty()) {
        println("Error: please set recipient")
        return
    }

    val recipients = args

    val properties = Properties()
    properties.load(ClassLoader.getSystemResourceAsStream("application.properties"))

    val smtpUsername = properties.getProperty("SmtpUser")
    val smtpPassword = properties.getProperty("SmtpPwd")

    smtpSend(
        recipients = recipients,
        from = "easylemon225@gmail.com",
        subject = "TEST TASK1",
//        message = """<!DOCTYPE html>
//<html lang="ru">
//  <head>
//    <meta charset="utf-8">
//    <meta name="viewport" content="width=device-width, initial-scale=1.0">
//    <title>Заголовок страницы</title>
//    <link rel="stylesheet" href="./styles/style.css">
//
//    <meta property="og:title" content="Заголовок страницы в OG">
//    <meta property="og:description" content="Описание страницы в OG">
//    <meta property="og:image" content="https://example.com/image.jpg">
//    <meta property="og:url" content="https://example.com/">
//  </head>
//  <body>
//    <header>
//      <h1>Sting - Desert Rose</h1>
//      <nav>
//        <ul>
//          <li><a href="index.html">(its html file)</a></li>
//        </ul>
//      </nav>
//    </header>
//    <main>
//      <article>
//        <section>
//          <p>This desert rose</p>
//          <p>Each of her veils, a secret promise</p>
//        </section>
//        <section>
//          <p>This desert flower</p>
//          <p>No sweet perfume ever tortured me more than this</p>
//        </section>
//      </article>
//    </main>
//    <footer>
//    </footer>
//    <!-- сюда можно подключить jquery <script src="scripts/app.js" defer></script> -->
//  </body>
//</html>""".trimIndent(),
        message = "STING - DESERT ROSE",
        smtpServer = "smtp.gmail.com",
        smtpPort = 587,
        smtpUsername = smtpUsername,
        smtpPassword = smtpPassword,
        isHtml = false,
    )
}

fun smtpSend(
    recipients: Array<String>,
    from: String,
    subject: String,
    message: String,
    smtpServer: String,
    smtpPort: Int,
    smtpUsername: String?,
    smtpPassword: String?,
    isHtml: Boolean
) {
    try {
        val props = Properties().apply {
            put("mail.smtp.host", smtpServer)
            put("mail.smtp.port", smtpPort.toString())
            put("mail.smtp.auth", "true")
            put("mail.smtp.starttls.enable", "true")
        }

        val session = Session.getInstance(props, object : Authenticator() {
            override fun getPasswordAuthentication(): PasswordAuthentication {
                return PasswordAuthentication(smtpUsername, smtpPassword)
            }
        })

        for (recipient in recipients) {
            val mailMsg = MimeMessage(session).apply {
                setFrom(InternetAddress(from))
                addRecipient(Message.RecipientType.TO, InternetAddress(recipient))
                this.subject = subject

                if (isHtml) {
                    setContent(message, "text/html; charset=utf-8")
                } else {
                    setText(message)
                }
            }

            Transport.send(mailMsg)
            println("SUCCESS")
        }
    } catch (e: MessagingException) {
        println("ERROR: ${e.message}")
    }
}
