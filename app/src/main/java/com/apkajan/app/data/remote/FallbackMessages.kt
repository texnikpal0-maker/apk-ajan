package com.apkajan.app.data.remote

/**
 * İnternet yoksa ya da uzak istek başarısız olursa kullanılan yerleşik SERT mesajlar.
 * {count} → bugünkü sayaç, {limit} → günlük limit ile değiştirilir.
 */
object FallbackMessages {

    val warnings: Map<String, List<String>> = mapOf(
        "az" to listOf(
            "Bəsdir! Artıq {count} video oldu. Baş barmağın təqaüdə çıxmaq istəyir, sən isə hələ də sürüşdürürsən.",
            "Limitin {limit} idi, sayğac isə {count}. Bu intizam deyil, təslimçilikdir.",
            "Telefonu yerə qoy. Həyatın bu ekranda keçmir.",
            "Daha bir video heç nəyi dəyişməyəcək. Bağla bunu. İndi!",
            "Alqoritm qazanır, sən isə uduzursan. Özünə gəl!",
            "Bu qədər video əvəzinə on səhifə kitab oxuya bilərdin. Dayan!",
            "Vaxtını başqasının kontentinə xərcləyirsən. Öz həyatın nə vaxt başlayacaq?"
        ),
        "tr" to listOf(
            "Yeter artık! Sayaç {count}. Başparmağın emekli olmak istiyor, sen hâlâ kaydırıyorsun.",
            "Limitin {limit} idi, sayaç {count} gösteriyor. Bu disiplin değil, teslimiyet.",
            "Telefonu bırak. Hayatın bu ekranda geçmiyor.",
            "Bir video daha hiçbir şeyi değiştirmeyecek. Kapat şunu. Hemen!",
            "Algoritma kazanıyor, sen kaybediyorsun. Toparlan!",
            "Bu kadar video yerine on sayfa kitap okurdun. Dur artık!",
            "Zamanını başkasının içeriğine harcıyorsun. Kendi hayatın ne zaman başlayacak?"
        ),
        "en" to listOf(
            "Enough! {count} videos already. Your thumb wants to retire and you're still scrolling.",
            "Your limit was {limit}. You're at {count}. That's not discipline, that's surrender.",
            "Put the phone down. Your life isn't happening on this screen.",
            "One more video won't change anything. Close it. Now!",
            "The algorithm is winning and you're losing. Get it together!",
            "You could have read ten pages by now. Stop scrolling!",
            "You're spending your time on other people's content. When does your own life start?"
        ),
        "ru" to listOf(
            "Хватит! Уже {count} видео. Твой палец просится на пенсию, а ты всё листаешь.",
            "Лимит был {limit}, а у тебя уже {count}. Это не дисциплина, это капитуляция.",
            "Положи телефон. Твоя жизнь проходит не на этом экране.",
            "Ещё одно видео ничего не изменит. Закрывай. Сейчас же!",
            "Алгоритм выигрывает, а ты проигрываешь. Соберись!",
            "За это время ты мог прочитать десять страниц. Хватит листать!",
            "Ты тратишь время на чужой контент. Когда начнётся твоя собственная жизнь?"
        )
    )

    /** Pomodoro sırasında sosyal medya açılınca: kısa ve net. */
    val pomodoro: Map<String, String> = mapOf(
        "az" to "Pomodoro işləyir, sən işləmirsən!",
        "tr" to "Pomodoro çalışıyor, sen çalışmıyorsun!",
        "en" to "Pomodoro is running, you're not working!",
        "ru" to "Помодоро идёт, а ты не работаешь!"
    )
}
