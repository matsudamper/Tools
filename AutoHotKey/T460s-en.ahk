#UseHook

;CapsLock
SetCapsLockState, AlwaysOff

Shift & vkF0sc03A::
send,{CapsLock}
return

;Language
RAlt::
send,{vkF3sc029}
return

;Shortcut
CapsLock & 4::
send,!{F4}
return

;cursor
CapsLock & j::
if GetKeyState("shift", "P")
    send,+{Up}
else
    send,{Up}
return

CapsLock & h::
if GetKeyState("shift", "P")
    send,+{Left}
else
    send,{Left}
return

CapsLock & l::
if GetKeyState("shift", "P")
    send,+{Right}
else
    send,{Right}
return

CapsLock & k::
if GetKeyState("shift", "P")
    send,+{Down}
else
    send,{Down}
return

CapsLock & g::
if GetKeyState("shift", "P")
    send,+{Home}
else
    send,{Home}
return

CapsLock & vkBAsc027::
if GetKeyState("shift", "P")
    send,+{End}
else
    send,{End}
return