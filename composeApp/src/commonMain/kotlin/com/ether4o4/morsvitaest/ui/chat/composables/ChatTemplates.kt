package com.ether4o4.morsvitaest.ui.chat.composables

/**
 * A reusable starting prompt offered from the new-chat empty state. Picking one drops
 * its [prompt] into the message box (cursor at the end) for the user to fill in and send
 * — it does not auto-send. These are intentionally generic; users keep their own private
 * templates elsewhere and paste them in as needed.
 */
data class ChatTemplate(
    val title: String,
    val description: String,
    val prompt: String,
)

/** Built-in, general-purpose templates shown to every user. */
val CHAT_TEMPLATES: List<ChatTemplate> = listOf(
    ChatTemplate(
        title = "Code",
        description = "Working code first, short why after",
        prompt = "You're a senior engineer. Give me working code first, then a brief why. " +
            "Adapt to my stack and only ask if something is truly blocking.\n\n" +
            "What I'm building or the bug I'm hitting:\n",
    ),
    ChatTemplate(
        title = "Explain",
        description = "Break something down simply",
        prompt = "Explain this clearly and simply — one concrete example, and why it matters. " +
            "Assume I'm smart but new to it.\n\nTopic:\n",
    ),
    ChatTemplate(
        title = "Research",
        description = "Briefing with options and a recommendation",
        prompt = "Research this and give me a tight briefing: the key facts, the best 2-3 options " +
            "with trade-offs, and your recommendation. Cite sources.\n\nTopic:\n",
    ),
    ChatTemplate(
        title = "Write a message",
        description = "Draft an email, text, or post",
        prompt = "Write this for me in a clear, natural voice — give me two short versions I can " +
            "choose from.\n\nWhat it's for (audience, goal, key points):\n",
    ),
    ChatTemplate(
        title = "Plan it",
        description = "Turn a goal into ordered steps",
        prompt = "Turn this goal into a concrete plan: the steps in order, what to do first today, " +
            "and anything I'm likely to overlook.\n\nGoal:\n",
    ),
    ChatTemplate(
        title = "Shell task",
        description = "Run it end-to-end in the Linux sandbox",
        prompt = "Do this end-to-end in the Linux sandbox: run the full set of commands in one go, " +
            "fix any errors yourself, and report the result. This is Alpine Linux " +
            "(apk, pip, python3) — not Termux.\n\nTask:\n",
    ),
)
