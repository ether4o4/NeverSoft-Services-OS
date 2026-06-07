package com.ether4o4.morsvitaest.ui.compare

/** Which side of the compare view a model / message belongs to. */
enum class ComparePane { A, B }

/** A single model reply in a compare exchange, attributed to a pane. */
data class CompareMessage(
    val pane: ComparePane,
    val text: String,
)

/**
 * Drives one "rotation" of a side-by-side model comparison.
 *
 * - Plain compare ([merge] = false): each pane answers the seed once, independently —
 *   neither model sees the other's reply.
 * - Merge ([merge] = true): the two models reply to each other, capped at
 *   [repliesPerRotation] replies EACH (default 2). One rotation therefore runs
 *   A, B, A, B and then stops and waits for the next user message — the rate limit that
 *   keeps the auto-conversation from running away.
 *
 * The actual model call is injected as [ask] (pane + built prompt -> reply text), so the
 * orchestration stays pure and unit-testable with no network or DI. Replies are pushed
 * through [onMessage] as they arrive so the UI can stream them in.
 */
class CompareOrchestrator(
    private val repliesPerRotation: Int = 2,
) {
    suspend fun runRotation(
        seed: String,
        labelA: String,
        labelB: String,
        merge: Boolean,
        ask: suspend (pane: ComparePane, prompt: String) -> String,
        onMessage: suspend (CompareMessage) -> Unit,
    ) {
        if (!merge) {
            onMessage(CompareMessage(ComparePane.A, ask(ComparePane.A, seed)))
            onMessage(CompareMessage(ComparePane.B, ask(ComparePane.B, seed)))
            return
        }

        val transcript = mutableListOf<CompareMessage>()

        suspend fun turn(pane: ComparePane) {
            val prompt = buildPrompt(pane, seed, transcript, labelA, labelB)
            val message = CompareMessage(pane, ask(pane, prompt))
            transcript.add(message)
            onMessage(message)
        }

        repeat(repliesPerRotation) {
            turn(ComparePane.A)
            turn(ComparePane.B)
        }
    }

    /**
     * Builds the prompt for [self]'s next turn. The first turn (empty transcript) gets the
     * bare seed; later turns get the original question plus the running cross-talk and an
     * instruction to respond to the other model.
     */
    private fun buildPrompt(
        self: ComparePane,
        seed: String,
        transcript: List<CompareMessage>,
        labelA: String,
        labelB: String,
    ): String {
        if (transcript.isEmpty()) return seed

        val selfLabel = if (self == ComparePane.A) labelA else labelB
        val otherLabel = if (self == ComparePane.A) labelB else labelA
        return buildString {
            append("Original question:\n").append(seed).append("\n\nConversation so far:\n")
            for (message in transcript) {
                val who = if (message.pane == ComparePane.A) labelA else labelB
                append(who).append(": ").append(message.text).append("\n")
            }
            append("\nYou are \"").append(selfLabel).append("\". Respond to ")
                .append(otherLabel).append("'s latest point — agree, refine, or push back. Be concise.")
        }
    }
}
