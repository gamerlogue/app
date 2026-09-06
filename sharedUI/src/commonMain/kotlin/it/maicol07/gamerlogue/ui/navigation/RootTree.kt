package it.maicol07.gamerlogue.ui.navigation

import io.github.fopwoc.nav3ksp.annotation.Tree

/**
 * The app's single navigation tree. nav3ksp generates, in the `…ui.navigation.rootTree` package:
 * `RootNavTree` (one `NavKey` per `@Branch` composable), `RootNavTreeBuilder` (the entries) and
 * `RootNavTreeLayout` (the polymorphic serializers module for the back stack).
 */
@Tree
annotation class RootTree
