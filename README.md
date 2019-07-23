# UnityMediaPicker

Image picker for Unity iOS/Android

## Environment

| Platform | OS |  IDE |
| --- | --- | --- |
| iOS | 12.1 | Xcode 10.1 |
| Android | 9 | -- |

## Getting Started

This extension is based on : https://github.com/thedoritos/unimgpicker
For camera part : https://medium.com/@datdeveloper/how-to-make-android-plugin-for-unity-take-photo-from-camera-and-gallery-c12fe247c770

## Demo

Choose from gallery or take from camera, create texture and render it on canvas image.

```csharp
using System;
using System.Collections;
using System.Collections.Generic;
using UnityEngine;
using UnityEngine.UI;

public class UnityMediaPicker : MonoBehaviour
{
    public Image PhotoZone;

    private int ImageRectHeight = 833, ImageRectWidth = 1480;

    public delegate void ImageDelegate(string path);

    public delegate void ErrorDelegate(string message);

    public event ImageDelegate Completed;

    public event ErrorDelegate Failed;

    private UnityMediaPicker imagePicker;
    void Awake()
    {
        imagePicker = this;

        ImageRectWidth = Convert.ToInt32(PhotoZone.rectTransform.rect.width);
        ImageRectHeight = Convert.ToInt32(PhotoZone.rectTransform.rect.height);

        imagePicker.Completed += (string path) =>
        {
            StartCoroutine(LoadImage(path, PhotoZone));
        };
    }

    public void OnPressShowPicker()
    {
        Show("Choose a Photo ...", "UnityMediaPicker", ImageRectWidth);
    }

    public void OnPressShowCapture()
    {
        Capture("Select a Camera ...", "UnityMediaPicker", ImageRectWidth);
    }

    private IEnumerator LoadImage(string path, Image output)
    {
        var url = "file://" + path;
        var www = new WWW(url);
        yield return www;

        var texture = www.texture;
        if (texture == null)
        {
            Debug.LogError("Failed to load texture url:" + url);
        }

        output.overrideSprite = Sprite.Create(texture, new Rect(0.0f, 0.0f, texture.width, texture.height), new Vector2(0.5f, 0.5f), 100.0f);

        float multiplayerX = texture.width / (float) ImageRectWidth;
        float multiplayerY = texture.height / (float) ImageRectHeight;

        if (multiplayerX >= multiplayerY)
        {
            if (multiplayerX > 1)
            {
                output.rectTransform.sizeDelta = new Vector2(texture.width / multiplayerX, texture.height / multiplayerX);
            }
            else
            {
                float strech = (float) ImageRectWidth / texture.width;
                output.rectTransform.sizeDelta = new Vector2(texture.width * strech, texture.height * strech);
            }
        }
        else
        {
            if (multiplayerY > 1)
            {
                output.rectTransform.sizeDelta = new Vector2(texture.width / multiplayerY, texture.height / multiplayerY);
            }
            else
            {
                float strech = (float) ImageRectHeight / texture.height;
                output.rectTransform.sizeDelta = new Vector2(texture.width * strech, texture.height * strech);
            }
        }
    }

    private IPicker picker =
#if UNITY_IOS && !UNITY_EDITOR
            new PickeriOS();
#elif UNITY_ANDROID && !UNITY_EDITOR
            new PickerAndroid();
#elif UNITY_EDITOR_OSX || UNITY_EDITOR_WIN
            new Picker_editor();
#else
            new PickerUnsupported();
#endif
    public void Show(string title, string outputFileName, int maxSize)
    {
        picker.Show(title, outputFileName, maxSize);
    }

    public void Capture(string title, string outputFileName, int maxSize)
    {
        picker.Capture(title, outputFileName, maxSize);
    }

    private void OnComplete(string path)
    {
        var handler = Completed;
        if (handler != null)
        {
            handler(path);
        }
    }

    private void OnFailure(string message)
    {
        var handler = Failed;
        if (handler != null)
        {
            handler(message);
        }
    }
}
```

## Development

- The Android project depends on OSX
    - Because it loads `classes.jar` from the Unity Application path.

