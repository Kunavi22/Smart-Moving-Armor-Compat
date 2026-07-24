# Smart Moving Armor Compat

Standalone Minecraft 1.7.10 client compatibility addon for Smart Moving / Smart Render armor animation issues.

## What this does

RenderPlayerAPI is enough for a clean addon. The hook point is `RenderPlayerAPI.afterSetArmorModel`, which runs after Forge/vanilla/custom armor selection has put the armor model in `renderPassModel`.

The compatibility code does not import Thaumcraft, Botania, Witching Gadgets, or any other armor mod. Instead it works on any returned `ModelBiped` armor model:

1. It reads Smart Moving's current animated main player model from RenderPlayerAPI.
2. It replaces the armor model's biped part fields with lightweight `ModelRenderer` proxies.
3. When the armor model renders its head/body/arms/legs, each proxy copies the live Smart Moving rotations into the original armor part immediately before drawing it.

That immediate-before-render copy matters because doing a simple copy in `afterSetArmorModel` is too early: most armor models call `setRotationAngles()` inside their own `render()` method and would otherwise overwrite the copied pose.

## Why this is preferred over ASM

No Thaumcraft jar patching is needed, and no Smart Moving jar patching is needed. RenderPlayerAPI already owns the relevant player armor render hook, so the addon can stay small and mod-agnostic.

ASM would only be worth considering if you need perfect support for a specific armor renderer that does not render through the standard `ModelBiped` part fields at all.

## Build notes

Use a normal ForgeGradle 1.7.10 workspace:

- Minecraft `1.7.10`
- Forge `10.13.4.1614`
- Java 7 or Java 8 source compatibility
- Add `RenderPlayerAPI-1.7.10-1.4.jar`, `SmartRender-1.7.10-2.1.jar`, and `SmartMoving-1.7.10-15.8.2-maka.jar` as compile/runtime libraries.

The source uses MCP method names (`render`, `postRender`, `addChild`) and also looks up both MCP and SRG field names at runtime, so it is tolerant of 1.7.10 deobf/obf naming differences in armor model fields.

The addon registers its RenderPlayerAPI base during FML `init`, after `RenderPlayerAPI`, `SmartRender`, and `SmartMoving`. That matters because SmartRender and SmartMoving also register their RenderPlayerAPI bases during `init`.

## Expected coverage

This should cover Thaumcraft 4.2.3.5 fortress armor, robes, cultist armor, hover harness, and other mods that return a custom `ModelBiped` from `ItemArmor.getArmorModel`, provided their renderer ultimately draws through the normal biped part fields.

It intentionally does not replace armor geometry. It only synchronizes transforms.

The implementation copies the animated biped part rotations into the original armor geometry immediately before each part renders. It does not try to fully re-render arbitrary armor inside SmartRender's internal torso/shoulder hierarchy; doing that would make the addon much more SmartRender-specific and less compatible with other armor mods.
