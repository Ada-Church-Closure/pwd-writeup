set $address = 0

break *main + 704
commands
    silent
    set $address = $rsi
    continue
end

break *main + 757
commands
    silent
    printf "Value at $rsi = 0x%llx\n", *(unsigned long long*)$address
    continue
end

run
continue




