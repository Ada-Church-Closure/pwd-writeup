.intel_syntax noprefix
.global _start
_start:
xor rax, rax
mov rdx, 0x0
loop:
cmp rdx, rsi
jae endcase
add rax, [rdi + 8 * rdx]
inc rdx
jmp loop
endcase:
xor rdx, rdx
div rsi


