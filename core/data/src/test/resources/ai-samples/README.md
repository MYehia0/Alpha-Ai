# AI live-check samples

Used only by `LiveCapabilityCheck`, which is skipped unless it is run with `-PliveAi`.

- `grammar.txt`, `translation.txt`, `summarization.txt`: three Arabic and three English
  samples each. One sample per language contains instruction-like text.
- `images/`: printed-text images, **to be added**. Two per language, named `ar-1.png`,
  `ar-2.png`, `en-1.png`, `en-2.png` (`.jpg` and `.webp` also work), each with the text it
  shows in a file of the same name ending in `.txt`.
- `audio/`: short voice clips, **to be added**. Two per language, named `ar-1.wav`,
  `en-1.wav`, and so on (`.mp3` and `.m4a` also work), each under 60 seconds, with the words
  spoken in a `.txt` file of the same name.

Do not put anything private in these files: they are sent to the model providers and are
committed to the repository.
