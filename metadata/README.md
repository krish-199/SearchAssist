# F-Droid Metadata

This directory contains metadata for F-Droid app store submission.

## Structure

```
metadata/en-US/
├── title.txt                    # App title (50 chars max)
├── short_description.txt         # Short summary (80 chars max)
├── full_description.txt          # Full app description (4000 chars max)
├── changelogs/
│   └── 9.txt                    # Changelog for version code 9
└── images/
    └── phoneScreenshots/         # Screenshots (PNG/JPG, 320-3840px)
        ├── 1.png
        ├── 2.png
        └── 3.png
```

## Files

- **title.txt**: The app name as it appears in F-Droid
- **short_description.txt**: A brief one-line description
- **full_description.txt**: Detailed description with features (supports basic HTML)
- **changelogs/[versionCode].txt**: What's new in each version
- **images/phoneScreenshots/**: Screenshots shown in the F-Droid listing

## Submission

To submit to F-Droid:
1. Fork the F-Droid Data repository: https://gitlab.com/fdroid/fdroiddata
2. Add a metadata file for your app in `metadata/com.krishdev.searchassist.yml`
3. Submit a merge request

The app metadata in this directory is compatible with both F-Droid and fastlane (for Google Play).

## References

- F-Droid Metadata Format: https://f-droid.org/docs/Build_Metadata_Reference/
- F-Droid Submission: https://f-droid.org/docs/Submitting_to_F-Droid/
- Fastlane Supply: https://docs.fastlane.tools/actions/supply/
