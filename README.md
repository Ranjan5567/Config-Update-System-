Redesign my existing Streamlit application, Digit Insurance Data Standardization Portal, to create a clean, modern, professional enterprise dashboard. Modify the existing codebase directly and preserve all existing functionality.

1. Overall Page Layout

Create a compact white navigation header with the Digit logo on the left and the existing Deploy option on the right.

Below the header, display a prominent page title, "Data Standardization Portal", with the subtitle "Map, validate, and prepare member records for destination systems." Place this title section on a subtle light-grey gradient background.

Below the title section, organize the main content into two side-by-side panels. The left panel should be narrow and contain configuration and file-upload controls. The right panel should be substantially wider and contain the column-mapping interface.

Both panels should have white backgrounds, subtle borders, rounded corners, consistent internal spacing, and a professional enterprise appearance.

2. Left Panel: Configure & Upload

Create a card titled "Configure & Upload" containing the following elements in this order:

- Product Code: A full-width dropdown with the available product codes. Retain the existing selection logic and display the selected product code.
- Policy Type: Display the available policy types, including ESCP and Parents Policy, as compact, rounded selection buttons or chips. Preserve the existing selection behavior.
- Upload Source File: Provide a large drag-and-drop upload area with a centered upload icon and a short instruction. Allow users to select files through the standard file picker as well.
- Submit & Map: Add a prominent, full-width dark-charcoal button with white text and rounded corners.

Use consistent spacing between the labels, controls, and sections. Keep the entire configuration form inside the left panel rather than allowing it to occupy the full page width.

3. Right Panel: Column Mapping

Create a large white card titled "Column Mapping".

At the top of the card, display breadcrumb navigation in this format:

Home > Configure & Upload > Column Mapping

Below the breadcrumb, show the section heading "Column Mapping", followed by a short description displaying the uploaded source filename and explaining that required destination fields are marked with an asterisk.

The filename must come dynamically from the uploaded file.

4. Column Mapping Table

Create a clean, structured table with three columns:

- Destination field
- Source column and suggestion
- Fallback/default value

The destination field column should display the field names. The source column should contain a dropdown and a suggestion message beneath it. The fallback column should contain a text input.

Include these required destination fields:

- Employee ID
- Member Name
- Date of Birth
- Gender
- Relationship to Employee

Mark required fields clearly using a consistent asterisk convention.

Style the table header with a subtle light-grey background, readable typography, and medium-weight text. Use a white background for the table body, thin horizontal dividers between rows, and consistent alignment and padding.

Keep the three columns aligned throughout the table. The destination field names should be left-aligned, the source dropdowns should occupy the available space in their column, and the fallback inputs should align horizontally with their corresponding dropdowns.

Each source dropdown must contain the column headers extracted from the uploaded Excel file. Each fallback input should display the placeholder "Enter a fallback value".

Display mapping suggestions directly below their corresponding dropdowns. Suggestions and confidence scores must come from the existing mapping logic or actual stored mapping history; do not hardcode or fabricate them.

5. Visual Style

Use a restrained, professional visual style appropriate for an internal insurance operations application.

- Use a white and light-grey background palette.
- Use dark-charcoal text for headings and labels.
- Use subtle borders and shadows to distinguish the two main panels from the page background.
- Use rounded corners for panels, buttons, dropdowns, and input fields.
- Use a dark-charcoal primary button with white text.
- Maintain a clear visual hierarchy between the page title, panel headings, field labels, and helper text.
- Keep the interface compact and readable without excessive whitespace.
- Avoid unnecessary decorative graphics, animations, and bright colors.

The final design should feel polished, consistent, and easy to use.

6. Functional Requirements

Preserve the existing functionality for:

- Product code selection.
- Policy type selection.
- Excel file upload and parsing.
- Source-column extraction.
- Automatic column-mapping suggestions.
- Manual mapping selection.
- Fallback/default value entry.
- Required-field validation.
- Application state persistence.
- Submit & Map behavior.

Do not replace working application logic with static mock data. Do not create a separate application unless absolutely necessary.

7. Implementation Instructions

First inspect the existing project structure and identify the main Streamlit application file.

Use the existing technology stack and dependencies. If the application is built with Streamlit, use its layout containers and columns to create the two-panel structure, and apply carefully scoped custom CSS to refine the styling.

Refactor the existing layout so that configuration controls are rendered in the left panel and the column-mapping table is rendered in the right panel. Do not merely change the colors and fonts of the current vertically stacked layout.

Ensure that interactive widgets retain their values across reruns and that the mapping table remains usable with different Excel files.

Make the layout responsive so that the panels stack vertically on narrow screens while remaining side by side on desktop and laptop screens.

8. Completion Criteria

Implement the changes directly in the existing source code, run the application, and verify the result.

The final application must have a compact navigation header, a prominent title section, a narrow configuration panel on the left, and a wider column-mapping panel on the right. The mapping table must have three aligned columns, with source dropdowns and fallback inputs displayed side by side.

Retain the complete upload-to-mapping workflow, fix any layout or interaction issues, and provide a concise summary of the files modified and how to run the updated application.
