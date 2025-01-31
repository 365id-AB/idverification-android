### Custom Theme
 
You can use `IdVerification.setCustomTheme()` to apply a specific set of colors.  
Most of the parameters of this function are optional (except `poweredByLogo`), so you can use only those that suits you. Below you see an example of how you can use this function:

```kotlin
setCustomTheme(
    theme = IdVerificationTheme(
        primary = Color.Purple,
        onPrimary = Color.White,
        primaryContainer = Color.LightPurple,
        
        secondary = Color.Gray,
        onSecondary = Color.White,
        secondaryContainer = Color.LightGray,
        onSecondaryContainer = Color.DarkGray,
        
        tertiary = Color.Purple,
        onTertiary = Color.MiddleGray,
        tertiaryContainer = Color.LightPurple,
        onTertiaryContainer = Color.White,
        
        surface = Color.White,
        onSurface = Color.Black,
        surfaceVariant = Color.LightGray,
        onSurfaceVariant = Color.DarkGray,
        inverseSurface = Color.Black,
        inverseOnSurface = Color.White,

        surfaceContainer = Color.lightGray,
        
        poweredByLogo = PoweredByLogo.BLACK,
        animations = Animations(),
    )
)
```

or alternately, if using Material Design 3 you can provide the desired color palette directly:
```kotlin
setCustomTheme(
    theme = IdVerificationTheme(
        colorScheme = myColorPalette,
        poweredByLogo = PoweredByLogo.BLACK,
        appBarLogo = R.drawable.MyAppbarLogo,
    )
)
```

Below you will find images showing which color variables are applied to certain elements in the SDK.
<br/>

## Information View
<img src="images/docsinformationview.png" width="500">

## Information Warning View
<img src="images/warning-view.png" width="500">

## Camera View
<img src="images/imagecaptureview.png" width="500">

## NFC view
<img src="images/docsnfcview.png" width="500">

## iProov view
<img src="images/facematchcameraview.png" width="500">

## Wait view
<img src="images/docswaitview.png" width="500">  

## Odd sized camera view
<img src="images/oddsizedcameraview.png" width="500">  

## Odd sized document captured view
<img src="images/oddsizedcapturedview.png" width="500">  

<br/>

You can use `Animations()` to set custom animations for preparation, loading and instructions.
```kotlin
Animations(
    prepareid3
    prepareId1Frontside
    prepareId1Backside
    prepareOddSizedDocument
    prepareDocument
    
    @Deprecated("This parameter is being phased out")
    prepareNfc
    prepareId1Nfc
    prepareId3Nfc
    prepareDocumentNfc
    prepareFacematch
    instructionId3
    instructionId1Frontside
    instructionId1Backside
    instructionDocument
    instructionOddSizedDocument
    
    @Deprecated("This parameter is being phased out")
    instructionNfc
    instructionId1Nfc
    instructionId3Nfc
    instructionDocumentNfc
    loadingImageCapture
    loadingNfc
    loadingFacematch
    loadingGeneric
)
```

- The prepare animations are shown before the step is to be performed.
- The instruction animations are shown during the step.
- The loading animations are shown after the step is performed.
- `prepareNfc` animation will override `prepareId1Nfc`, `prepareId3Nfc` and `prepareDocumentNfc`
- `instructionNfc` animation will override `instructionId1Nfc`, `instructionId3Nfc` and `instructionDocumentNfc`

<br/>

Here is an example of how to override the default animations, in this case by replacing a few of them with `ExampleLoadingSpinner()`.  
You can try this out in the [example project](example).
```kotlin
val theme = IdVerification.IdVerificationTheme(
            animations = IdVerification.Animations(
                prepareId3 = { ExampleLoadingSpinner() },
                loadingGeneric = { ExampleLoadingSpinner() },
                loadingImageCapture = { ExampleLoadingSpinner() }
            )
        )
        IdVerification.setCustomTheme(theme)
```
