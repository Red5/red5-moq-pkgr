# Catalog Architecture

## Status

This repository now treats MSF as the primary catalog direction. The older standalone catalog-format draft is not the target architecture here anymore.

Catalog code is organized into three layers:

1. Shared catalog model and JSON serializer base in `org.red5.io.moq.catalog`
2. Format-specific catalog APIs in `org.red5.io.moq.warp.catalog` and `org.red5.io.moq.msf.catalog`
3. Adapters in `org.red5.io.moq.catalog` for converting between the shared model and WARP/MSF models

## Shared Layer

The shared layer contains:

- `Catalog`
- `CatalogTrack`
- `CommonTrackFields`
- `SelectionParameters`
- `CatalogSerializer`

`CatalogSerializer` is now the common JSON serialization base used by the format-specific serializers.

The shared model has been widened to carry WARP/MSF-style fields, including:

- root fields such as `deltaUpdate`, `isComplete`, `generatedAt`, `addTracks`, `removeTracks`, `cloneTracks`
- track fields such as `role`, `parentName`, `trackDuration`, `isLive`, `targetLatency`, `type`, `eventType`, `maxGrpSapStartingType`, `maxObjSapStartingType`

## Format-Specific Layer

Format-specific code remains the user-facing API:

- Use `WarpCatalog`, `WarpTrack`, `WarpCatalogSerializer`, and `WarpCatalogValidator` for legacy WARP
- Use `MsfCatalog`, `MsfTrack`, `MsfCatalogSerializer`, and `MsfCatalogValidator` for MSF and CMSF

These serializers now inherit the shared `CatalogSerializer` JSON base instead of maintaining separate Gson implementations.

## Adapters

Adapters currently provided:

- `WarpCatalogAdapter`
- `MsfCatalogAdapter`

These are intended for:

- migration work
- shared-model interoperability
- future refactors that need to move between the generic/shared catalog model and format-specific APIs

## Guidance

- For new application code in this repository, prefer the format-specific APIs over the shared `Catalog` model.
- Use the shared `Catalog` model when building tooling, migrations, or serializer infrastructure.
- Keep validation in the format-specific validators, not in the shared serializer base.
