.intel_syntax noprefix
.global _start
_start:
mov rdi, 2  # 创建一个server的socket
mov rsi, 1
mov rdx, 0
mov rax, 0x29
syscall
mov rdi, 0  # 干净的退出这个程序
mov rax, 60
syscall
