# Leaflet Marker Assets

The following files need to be copied from `node_modules/leaflet/dist/images/`:

- `marker-icon.png`
- `marker-icon-2x.png`  
- `marker-shadow.png`

To copy them, run:

```bash
cp node_modules/leaflet/dist/images/marker-icon.png apps/dashboard/src/assets/
cp node_modules/leaflet/dist/images/marker-icon-2x.png apps/dashboard/src/assets/
cp node_modules/leaflet/dist/images/marker-shadow.png apps/dashboard/src/assets/
```

Or add this to your angular.json assets configuration:

```json
{
  "glob": "**/*",
  "input": "node_modules/leaflet/dist/images",
  "output": "assets"
}
```
