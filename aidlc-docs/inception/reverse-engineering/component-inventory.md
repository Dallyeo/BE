# Component Inventory

## Application Packages
- `com.ppip.dallyeo` — Application root; contains `DallyeoApplication` bootstrap only.

## Infrastructure Packages
- None (no CDK / Terraform / CloudFormation present).

## Shared Packages
- None (no separate models/utilities/clients modules; single-module project).

## Test Packages
- `com.ppip.dallyeo` (test source set) — `DallyeoApplicationTests` (context-load smoke test).

## Total Count
- **Total Packages**: 1 (`com.ppip.dallyeo`, spanning main + test source sets)
- **Application**: 1
- **Infrastructure**: 0
- **Shared**: 0
- **Test**: 1 (test source set within the same package)

## Source File Count
- **Main Java files**: 1 (`DallyeoApplication.java`)
- **Test Java files**: 1 (`DallyeoApplicationTests.java`)
- **Resource/config files**: 1 (`application.properties`)
