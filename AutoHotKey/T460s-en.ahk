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

CapsLock & Right::
send,^#{Right}
return

CapsLock & Left::
send,^#{Left}
return

;cursor
CapsLock & f::
if GetKeyState("shift", "P")
    send,+{Up}
else
    send,{Up}
return

CapsLock & g::
if GetKeyState("shift", "P")
    send,+{Left}
else
    send,{Left}
return

CapsLock & h::
if GetKeyState("shift", "P")
    send,+{Right}
else
    send,{Right}
return

CapsLock & j::
if GetKeyState("shift", "P")
    send,+{Down}
else
    send,{Down}
return

CapsLock & d::
if GetKeyState("shift", "P")
    send,+{Home}
else
    send,{Home}
return

CapsLock & k::
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