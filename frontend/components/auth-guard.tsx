"use client";

import { useEffect, useState } from "react";
import { useRouter, usePathname } from "next/navigation";
import { isAuthenticated } from "@/lib/auth";

export function AuthGuard({ children }: { children: React.ReactNode }) {
  const router = useRouter();
  const pathname = usePathname();
  const [isChecking, setIsChecking] = useState(true);

  useEffect(() => {
    // Don't check auth on login page
    if (pathname === "/login") {
      setIsChecking(false);
      return;
    }

    // Check if user is authenticated
    if (!isAuthenticated()) {
      router.push("/login");
    } else {
      setIsChecking(false);
    }

    const handleVisibilityChange = () => {
      if (document.visibilityState === "visible" && !isAuthenticated()) {
        router.push("/login");
      }
    };

    document.addEventListener("visibilitychange", handleVisibilityChange);
    return () => {
      document.removeEventListener("visibilitychange", handleVisibilityChange);
    };
  }, [pathname, router]);

  // Show loading or nothing while checking
  if (isChecking && pathname !== "/login") {
    return (
      <div className="flex min-h-screen items-center justify-center">
        <div className="text-muted-foreground">Carregando...</div>
      </div>
    );
  }

  return <>{children}</>;
}
