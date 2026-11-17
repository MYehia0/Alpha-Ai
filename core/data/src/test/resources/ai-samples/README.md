# AI live-check samples

Used only by `LiveCapabilityCheck`, which is skipped unless it is run with `-PliveAi`.

- `grammar.txt`, `translation.txt`, `summarization.txt`: three Arabic and three English
  samples each. One sample per language contains instruction-like text.
- `images/`: printed-text images, two per language (`ar-1.png`, `en-1.png`, ...), each with
  the text it shows in a `.txt` file of the same name. They were rendered in Tahoma with
  `System.Drawing` on Windows. `.jpg` and `.webp` also work.
- `audio/`: short voice clips, two per language (`ar-1.wav`, `en-1.wav`, ...), each with the
  words spoken in a `.txt` file of the same name. They were made with the Windows speech
  voices (Hoda for Arabic, David for English). `.mp3` and `.m4a` also work; keep each under
  60 seconds.

Do not put anything private in these files: they are sent to the model providers and are
committed to the repository.
