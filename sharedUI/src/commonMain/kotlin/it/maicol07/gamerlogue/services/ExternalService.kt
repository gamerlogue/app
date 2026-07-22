package it.maicol07.gamerlogue.services

import kotlinx.serialization.Serializable

/**
 * Identity of an external gaming platform — a stable key for DI lookup, navigation, settings keys and
 * UI iteration. All per-service data and behaviour live on its [ServiceConnector]; icon/label are
 * mapped in the UI layer.
 */
@Serializable
enum class ExternalService { STEAM, PLAYSTATION, XBOX, GOG, EPIC, NINTENDO, UBISOFT }
