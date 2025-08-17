set $address = 0

break *main + 570
commands
    silent
    set $address = $rsi
    continue
end

break *main + 620
commands
    silent
    if($address != 0)
        set *(unsigned long long*)$rsi = *(unsigned long long*)$address
    
    set $rip = *main + 630
    continue
end

start




