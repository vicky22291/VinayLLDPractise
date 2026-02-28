Process a review file by responding to all pending comments.

## Instructions

1. Run `review-cli list $ARGUMENTS` to see all comments with their status
2. For each comment with status "pending":
   a. Run `review-cli show $ARGUMENTS --comment N` to get the full context
   b. Read the source file at the specified lines to understand the code/document
   c. Research related systems and documentation if the comment requires broader context
   d. Formulate a detailed response with citations (file paths + line numbers)
   e. Run `review-cli respond $ARGUMENTS --comment N --response "your response"`
3. After all pending comments are processed, run `review-cli status $ARGUMENTS` to confirm completion

## Response Guidelines

- **Cite sources**: Always include file paths and line numbers (e.g., `FeatureManager.java:156-234`)
- **Be specific**: Reference actual code, not general concepts
- **Use diagrams**: Include Mermaid diagrams when explaining flows or architecture
- **Stay concise**: Aim for 2-5 paragraphs per response, focused on the question
- **Ask back**: If a comment is unclear, respond with a clarifying question rather than skipping
- **Code suggestions**: When relevant, include specific code changes in diff format

## Example

```bash
# Step 1: See what needs responding
review-cli list .review/docs--uscorer--ARCHITECTURE_OVERVIEW.review.json

# Step 2: Process each pending comment
review-cli show .review/docs--uscorer--ARCHITECTURE_OVERVIEW.review.json --comment 1
# ... read source, research ...
review-cli respond .review/docs--uscorer--ARCHITECTURE_OVERVIEW.review.json --comment 1 --response "Based on..."

# Step 3: Confirm
review-cli status .review/docs--uscorer--ARCHITECTURE_OVERVIEW.review.json
```
