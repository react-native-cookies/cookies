require "json"

package = JSON.parse(File.read(File.join(__dir__, "package.json")))

Pod::Spec.new do |s|
  s.name                = "react-native-cookies"
  s.version             = package["version"]
  s.summary             = package["description"]
  s.homepage            = package["homepage"]
  s.license             = package["license"]
  s.author              = { package["author"]["name"] => package["author"]["email"] }
  s.source              = { :git => "git@github.com:react-native-community/cookies.git", :tag => "v#{s.version}" }
  s.requires_arc        = true
  s.platform            = :ios, "7.0"
  s.preserve_paths      = "*.framework"
  s.source_files        = "ios/**/*.{h,m}"
  s.dependency "React-Core"
end
