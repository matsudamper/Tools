#UseHook

;CapsLock
SetCapsLockState, AlwaysOff

;Language
RAlt::
send,{vkF3sc029}
return

;Shortcut
CapsLock & 4::
send,!{F4}
return

;DeskTop Change
CapsLock & o::
send,^#{Right}
return

CapsLock & u::
send,^#{Left}
return

;cursor
CapsLock & i::
if GetKeyState("shift", "P")
    send,+{Up}
else
    send,{Up}
return

CapsLock & j::
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

CapsLock & h::
if GetKeyState("shift", "P")
    send,+{Home}
else
    send,{Home}
return

CapsLock & `;::
if GetKeyState("shift", "P")
    send,+{End}
else
    send,{End}
return

;copy & paste
CapsLock & z::
send,^{z}
return

CapsLock & x::
send,^{x}
return

CapsLock & c::
send,^{c}
return

CapsLock & v::
send,^{v}
return

;Develop
CapsLock & Enter::
send,^{Enter}
return

CapsLock & s::
send,^{s}
return

CapsLock & w::
send,^{w}
return
