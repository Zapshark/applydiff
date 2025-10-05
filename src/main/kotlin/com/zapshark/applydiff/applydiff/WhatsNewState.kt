package com.zapshark.applydiff

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.Service

@State(
    name = "ApplyDiff.WhatsNewState",
    storages = [Storage("ApplyDiffWhatsNew.xml")]
)
@Service(Service.Level.APP)
class WhatsNewState : PersistentStateComponent<WhatsNewState> {

    var lastShownVersion: String? = null

    override fun getState(): WhatsNewState = this

    override fun loadState(state: WhatsNewState) {
        this.lastShownVersion = state.lastShownVersion
    }
}
