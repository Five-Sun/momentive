import type { Metadata } from "next";
import { Noto_Sans_KR } from "next/font/google";
import localFont from "next/font/local";
import "./globals.css";

const uhbeeSehyun = localFont({
  src: "../../public/fonts/UhBeeSehyun.woff2",
  weight: "400",
  variable: "--font-uhbee",
  display: "swap",
});

const notoSansKr = Noto_Sans_KR({
  weight: ["400", "500", "600", "700"],
  subsets: ["latin"],
  variable: "--font-noto-sans-kr",
});

export const metadata: Metadata = {
  title: "모멘티브",
  description: "강아지 쇼핑몰",
};

export default function RootLayout({ children }: LayoutProps<"/">) {
  return (
    <html
      lang="ko"
      className={`h-full antialiased ${uhbeeSehyun.variable} ${notoSansKr.variable}`}
    >
      <body className="min-h-full flex flex-col">{children}</body>
    </html>
  );
}
