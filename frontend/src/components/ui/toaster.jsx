import { useToast } from "@/components/ui/use-toast";
import {
    Toast,
    ToastClose,
    ToastDescription,
    ToastProvider,
    ToastTitle,
    ToastViewport,
} from "@/components/ui/toast";

const TOAST_DURATION_MS = 3500;

export function Toaster() {
    const { toasts } = useToast();

    return (
        <ToastProvider swipeDirection="left">
            {toasts.map(({ id, title, description, action, ...props }) => (
                <Toast
                    key={id}
                    duration={TOAST_DURATION_MS}
                    {...props}
                >
                    <div className="grid gap-0.5 flex-1 min-w-0">
                        {title && <ToastTitle>{title}</ToastTitle>}
                        {description && <ToastDescription>{description}</ToastDescription>}
                    </div>
                    {action}
                    <ToastClose />
                </Toast>
            ))}
            <ToastViewport />
        </ToastProvider>
    );
}
