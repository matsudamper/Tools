#UseHook

SetCapsLockState, AlwaysOff

Shift & vkF0sc03A::
send,{CapsLock}
return

CapsLock & 4::
send,!{F4}
return

CapsLock & j::
if GetKeyState("shift", "P")
    send,+{Left}
else
    send,{Left}
return

CapsLock & i::
if GetKeyState("shift", "P")
    send,+{Up}
else
    send,{Up}
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

CapsLock & u::
if GetKeyState("shift", "P")
    send,+{Home}
else
    send,{Home}
return

CapsLock & o::
if GetKeyState("shift", "P")
    send,+{End}
else
    send,{End}
return