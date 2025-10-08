# MW Trait Reloader

Due to the FIOS (First In, Only Served) nature of how traits are loaded into the game engine, it is impossible for two mods that modify the traits in the same files to co-exist without causing compatibility issues. This is why this mod is uploaded here.

This mod changes nothing in terms of game balance. All it does is rewrite every trait into a separate file and clear the vanilla files. Therefore, modders can use this mod to avoid conflicts between their mods should they wish to modify traits.

How this mod works:

Traits in vanilla files will be written separately in their files, named with the format `vanillaFileName_appearanceOrder_traitName.txt`. For example:

In vanilla file `00_admiral_traits.txt`, the traits appear as the following order:

- `trait_ruler_corvette_focus`
- `trait_ruler_destroyer_focus`
- ...

This mod will clear all contents in the vanilla `00_admiral_traits.txt` file, and rewrite:

- trait `trait_ruler_corvette_focus` into file `00_admiral_traits_01_trait_ruler_corvette_focus.txt` because it is the 1st trait in the file.
- trait `trait_ruler_destroyer_focus` into file `00_admiral_traits_02_trait_ruler_destroyer_focus.txt` because it is the 2nd trait in the file.
- ...

So that all traits are still loaded into the game engine as the same order as they would in vanilla way.

Enough characters of `0` are added into the order part to ensure such order, so 11 does not load before 2.

## To Modders

When modding, the best practice is to follow the steps below:

1. Copy the vanilla file which contains the trait you want to modify to folder `processor/input/traits`
2. Edit the trait in the copied file. Do notice that, **DO NOT** add new traits (except new tiers of existing traits) in the middle of the file, because it will break the order of vanilla traits. If you want to add your own traits, just add them in your own files.
3. Use the Java program provided to generate the trait files. You will see a lot of trait files generated in folder `processor/output/traits`. Copy and only copy the files containing traits you modified to the related mod paths.