# Built-in identity chess

Chess is part of Xeres; no plugin installation is needed.

- In a chat room, right-click another participant and choose **Invite to chess**.
- In a distant chat window, choose **Invite to chess**.
- The recipient gets an invitation popup and can accept or decline. Draw offers also use a popup.
- The inviter plays White. The accepting player plays Black.
- Click a piece, then a highlighted destination. Promotion offers queen, rook, bishop or knight.
- Players can resign, offer/answer a draw, and claim a draw by repetition or the fifty-move rule.
- The window displays coordinate move history with Abort, Draw and Resign underneath. The options menu contains New invitation and Position details (copyable FEN and hash). Draw claims are available from the Draw button.
- Closing the window leaves the game available in the running Xeres instance. Use the chat chess action to reopen it. Use **Abort** to cancel an invitation or abort a game, or **Resign** to concede an active game.

Xeres currently has one local chat identity. Chess uses that same identity, and identifies opponents by their authenticated GXS identity rather than their nickname. No invitation is broadcast as ordinary room text.

## RetroChess compatibility

The implementation targets the RetroChess source supplied at `RetroShare/plugins/RetroChess` on 2026-09-06. Identity games use secured GXS tunnels with application service ID `0xC4E5`.

Supported packets include `chess_invite`, `chess_accept`, `chess_reject`, `player_leave` and `game_action`. Declining an incoming invitation sends `chess_reject`; receiving it resolves only an outgoing invitation and displays “Invitation declined.” Legacy `player_leave` replies also resolve outgoing invitations. Cancelling an outgoing invitation or leaving a game still sends `player_leave`. Verified moves use:

```
{"type":"game_action","action":"move:1:52:36:-:952a5e992e65efab"}
```

This is 1.e4: squares are numbered from a8 = 0 through h1 = 63; the sequence starts at 1; promotion is `-`, `Q`, `R`, `B` or `H` (knight). The hash is the first 16 lowercase hexadecimal characters of SHA-256 over the resulting six-field UTF-8 FEN. Legacy four-field `move:from:to:promotion` actions are accepted. Invalid moves, sequence mismatches and hash mismatches pause the game without applying the invalid position.

Chess attaches to an existing identity tunnel when distant chat is already open. Chess cleanup removes only chess's service registration and queued packets, preserving chat. Invitations expire after ten minutes.

## Current limits

- This targets identity games with the supplied RetroChess version. Direct location/SSL-peer games and old click-by-click move packets are not implemented.
- RetroChess rematch requests are declined; use a new invitation after ending a game.
- Games and move histories remain in memory for the lifetime of the running Xeres instance; restart recovery and PGN export are not implemented.
- Protocol fixtures and automated tests do not replace a live Xeres–RetroChess network game. Live interoperability still needs to be verified with two connected identities, including invitations in both directions, promotion, castling and closing chess while chat remains usable.

## Verification

```
gradlew :app:test --tests "io.xeres.app.xrs.service.chess.*" --tests "io.xeres.app.xrs.service.gxstunnel.ChessTunnelSharingTest"
gradlew :ui:test --tests "io.xeres.ui.controller.chess.*"
```

Rules tests cover the standard initial-position move tree, special moves, king safety, checkmate and the RetroChess FEN hash fixture. Protocol tests cover invitation consent, colors, verified packets, invalid sequence/hash rejection and draw acceptance. UI checks load all five supported languages. Tunnel tests check service sharing and cancellation before a tunnel connects.
