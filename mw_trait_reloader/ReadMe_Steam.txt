[h1]MW Trait Reloader (4.4.4)[/h1]

Due to the FIOS (First In, Only Served) nature of how traits are loaded into the game engine, it is impossible for two mods that modify the traits in the same files to co-exist without causing compatibility issues. This is why this mod is uploaded here.

This mod changes nothing in terms of game balance. All it does is rewrite every trait into a separate file and clear the vanilla files. Therefore, modders can use this mod to avoid conflicts between their mods should they wish to modify traits.

How this mod works:

Traits in vanilla files will be written separately in their files, named with the format [i]vanillaFileName_appearanceOrder_traitName.txt[/i]. For example:

In vanilla file [i]00_admiral_traits.txt[/i], the traits appear as the following order:

[list]
  [*] [i]trait_ruler_corvette_focus[/i]
  [*] [i]trait_ruler_destroyer_focus[/i]
  [*] ...
[/list]

This mod will clear all contents in the vanilla [i]00_admiral_traits.txt[/i] file, and rewrite:

[list]
  [*] trait [i]trait_ruler_corvette_focus[/i] into file [i]00_admiral_traits_01_trait_ruler_corvette_focus.txt[/i] because it is the 1st trait in the file.
  [*] trait [i]trait_ruler_destroyer_focus[/i] into file [i]00_admiral_traits_02_trait_ruler_destroyer_focus.txt[/i] because it is the 2nd trait in the file.
  [*] ...
[/list]

So that all traits are still loaded into the game engine as the same order as they would in vanilla way.

Enough characters of [i]0[/i] are added into the order part to ensure such order, so 11 does not load before 2.

GitHub Page: https://github.com/GHYNG/Stellaris-Trait-Reloader